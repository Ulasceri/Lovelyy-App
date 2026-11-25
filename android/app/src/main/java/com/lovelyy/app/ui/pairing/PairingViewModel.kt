package com.lovelyy.app.ui.pairing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.data.AppError
import com.lovelyy.app.data.AppException
import com.lovelyy.app.data.PairingRepository
import com.lovelyy.app.data.UserProfile
import kotlinx.coroutines.launch

class PairingViewModel(private val pairing: PairingRepository) : ViewModel() {

    enum class Tab { Share, Enter }

    var tab by mutableStateOf(Tab.Share)
    var input by mutableStateOf("")
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<AppError?>(null)
        private set
    private var autoCreated = false

    fun selectTab(t: Tab) {
        tab = t
        error = null
    }

    fun onInput(raw: String) {
        input = PairingRepository.normalize(raw).take(PairingRepository.CODE_LENGTH)
        error = null
    }

    /** Creates a code automatically the first time the Share tab is shown. */
    fun ensureCode(me: UserProfile) {
        if (autoCreated || me.pairCode != null) return
        autoCreated = true
        newCode(me)
    }

    fun newCode(me: UserProfile) = launchAction { pairing.createCode(me) }

    // On success, users/{me}.coupleId is set and AppViewModel switches to Home by itself.
    fun join(me: UserProfile) = launchAction { pairing.joinWithCode(me, input) }

    private fun launchAction(block: suspend () -> Unit) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: AppException) {
                error = e.error
            } finally {
                loading = false
            }
        }
    }
}
