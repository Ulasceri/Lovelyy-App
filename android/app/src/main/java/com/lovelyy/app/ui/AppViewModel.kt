package com.lovelyy.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
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
import kotlinx.coroutines.flow.stateIn

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

    val state: StateFlow<AppState> = buildState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Loading)

    private fun buildState(): Flow<AppState> {
        if (!app.isFirebaseConfigured) return flowOf<AppState>(AppState.FirebaseMissing)
        val auth = app.authRepository
        val users = app.userRepository

        return auth.authState.flatMapLatest { firebaseUser ->
            if (firebaseUser == null) return@flatMapLatest flowOf<AppState>(AppState.SignedOut)
            flow<AppState> {
                emit(AppState.Loading)
                users.ensureUserDoc(firebaseUser)
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
            }.catch { emit(AppState.Failed(it.message)) }
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
