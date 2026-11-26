package com.lovelyy.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.LeaveRepository
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface LeaveEvent {
    data object Failed : LeaveEvent
    /** Signed in too long ago to delete the account: they were signed out to sign in again. */
    data object SignInAgain : LeaveEvent
}

/**
 * Unpairing and deleting the account (Eşleşmeyi kaldır, Hesap). On success nothing to show here: the user doc
 * changes and AppViewModel moves to the pairing (or signed-out) screen by itself.
 */
class LeaveViewModel(private val app: LovelyyApplication) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val events = Channel<LeaveEvent>(Channel.BUFFERED)
    val oneTimeEvents = events.receiveAsFlow()

    fun unpair(me: UserProfile) = run {
        app.leaveRepository.unpair(app, me)
        Telemetry.track(AnalyticsEvent.CoupleUnpaired)
    }

    fun deleteAccount(me: UserProfile) = run {
        when (app.leaveRepository.deleteAccount(app, me)) {
            LeaveRepository.DeleteResult.Deleted -> Telemetry.track(AnalyticsEvent.AccountDeleted)
            LeaveRepository.DeleteResult.NeedsFreshSignIn -> {
                events.send(LeaveEvent.SignInAgain)
                app.authRepository.signOut(app)
            }
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Leaving failed")
                events.send(LeaveEvent.Failed)
            } finally {
                _busy.value = false
            }
        }
    }
}
