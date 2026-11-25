package com.lovelyy.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.analytics.screenName
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Which big screen the app should show. Decided only from auth + Firestore state. */
sealed interface AppState {
    data object Loading : AppState
    data object FirebaseMissing : AppState
    data object SignedOut : AppState
    data class NeedsName(val me: UserProfile) : AppState
    data class Pairing(val me: UserProfile) : AppState
    data class Home(val me: UserProfile, val couple: Couple?, val partner: UserProfile?) : AppState
    data class Failed(val message: String?) : AppState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val app: LovelyyApplication) : ViewModel() {

    // Screen tracking: the app navigates by state, so a new screen = a new state type.
    private var lastScreen: String? = null
    private var lastState: AppState? = null

    val state: StateFlow<AppState> = buildState()
        .onEach(::track)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    private fun track(s: AppState) {
        // Pairing -> Home means the couple was just formed (both phones see this once).
        if (lastState is AppState.Pairing && s is AppState.Home) Telemetry.track(AnalyticsEvent.PairingCompleted)
        if (s !is AppState.Loading) lastState = s
        val screen = s.screenName ?: return
        if (screen != lastScreen) {
            lastScreen = screen
            Telemetry.onScreen(screen)
        }
    }

    private fun buildState(): Flow<AppState> {
        if (!app.isFirebaseConfigured) return flowOf<AppState>(AppState.FirebaseMissing)
        val auth = app.authRepository
        val users = app.userRepository

        return auth.authState.flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                app.signalCenter.reset()
                return@flatMapLatest flowOf<AppState>(AppState.SignedOut)
            }
            flow<AppState> {
                emit(AppState.Loading)
                // Don't wait for the network before listening: the cached profile shows the home screen at once.
                viewModelScope.launch {
                    runCatching { users.ensureUserDoc(firebaseUser) }.onFailure { Timber.e(it, "Creating the user doc failed") }
                }
                emitAll(
                    users.observeUser(firebaseUser.uid).flatMapLatest { me ->
                        when {
                            me == null -> flowOf<AppState>(AppState.Loading)
                            me.displayName.isBlank() -> flowOf<AppState>(AppState.NeedsName(me))
                            me.coupleId == null -> flowOf<AppState>(AppState.Pairing(me))
                            else -> homeState(me, me.coupleId)
                        }
                    }
                )
            }.catch {
                Timber.e(it, "Loading the signed-in user failed")
                emit(AppState.Failed(it.message))
            }
        }
    }

    private fun homeState(me: UserProfile, coupleId: String): Flow<AppState> =
        app.userRepository.observeCouple(coupleId).flatMapLatest { couple ->
            val partnerUid = couple?.partnerOf(me.uid)
            if (partnerUid == null) {
                flowOf<AppState>(AppState.Home(me, couple, null))
            } else {
                app.userRepository.observeUser(partnerUid).map { partner -> AppState.Home(me, couple, partner) }
            }
        }
}
