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
import com.lovelyy.app.telemetry.trace
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
    /** When the code I'm sharing stops working (millis), once known. */
    var expiresAt by mutableStateOf<Long?>(null)
        private set
    private var autoCreated = false
    private var expiryFor: String? = null

    fun selectTab(t: Tab) {
        tab = t
        error = null
    }

    fun onInput(raw: String) {
        input = PairingRepository.normalize(raw).take(PairingRepository.CODE_LENGTH)
        error = null
    }

    fun press(digit: Char) {
        if (input.length < PairingRepository.CODE_LENGTH) onInput(input + digit)
    }

    fun backspace() {
        input = input.dropLast(1)
        error = null
    }

    /** Creates a code the first time the Share page is shown, and learns when it expires. */
    fun ensureCode(me: UserProfile) {
        val code = me.pairCode
        if (code == null) {
            if (!autoCreated) {
                autoCreated = true
                newCode(me)
            }
            return
        }
        if (expiryFor == code) return
        expiryFor = code
        viewModelScope.launch {
            val created = runCatching { pairing.codeCreatedAt(code) }.getOrNull()
            if (created == null || System.currentTimeMillis() - created > PairingRepository.CODE_TTL_MS) {
                newCode(me) // gone or too old: quietly make a fresh one
            } else {
                expiresAt = created + PairingRepository.CODE_TTL_MS
            }
        }
    }

    fun newCode(me: UserProfile) {
        expiresAt = null
        launchAction { trace("pairing_create_code") { pairing.createCode(me) } }
    }

    // On success, users/{me}.coupleId is set and AppViewModel switches to Home by itself.
    fun join(me: UserProfile) = launchAction { trace("pairing_join") { pairing.joinWithCode(me, input) } }

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
