package com.lovelyy.app.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.analytics.InnerScreens
import com.lovelyy.app.data.AppError
import com.lovelyy.app.data.AppException
import com.lovelyy.app.data.AuthRepository
import com.lovelyy.app.data.UserRepository
import com.lovelyy.app.telemetry.Telemetry
import kotlinx.coroutines.launch

class LoginViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    /** Welcome (Karşılama) → Intro (try-it tanıtım, only via "Başlayalım") → SignUp (Kayıt) ⇄ SignIn (Giriş). */
    enum class Mode { Welcome, Intro, SignIn, SignUp }

    var mode by mutableStateOf(Mode.Welcome)
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<AppError?>(null)
        private set
    /** Shown after "Şifremi unuttum" worked. */
    var resetSent by mutableStateOf(false)
        private set

    /** "Başlayalım" on Karşılama: the intro comes before the sign-up form. */
    fun startIntro() {
        switchMode(Mode.Intro)
        Telemetry.onScreen(InnerScreens.INTRO)
    }

    /** The intro ended, through its last page or "Atla"; either way the sign-up form is next. */
    fun finishIntro(skipped: Boolean, demosTried: Int) {
        Telemetry.track(AnalyticsEvent.IntroFinished(skipped, demosTried))
        Telemetry.onScreen("login")
        switchMode(Mode.SignUp)
    }

    fun switchMode(newMode: Mode) {
        mode = newMode
        error = null
        resetSent = false
    }

    /** Back to an empty Karşılama: nothing typed for the previous sign-in may show up after signing out. */
    fun reset() {
        switchMode(Mode.Welcome)
        name = ""
        email = ""
        password = ""
    }

    /** Called when a field is edited, so a stale error doesn't linger until resubmit. */
    fun clearError() { if (error != null) error = null }

    fun google(activityContext: Context) = launchAction { auth.signInWithGoogle(activityContext) }

    fun submitEmail() {
        if (email.isBlank()) { error = AppError.InvalidEmail; return }
        if (password.length < 6) { error = AppError.WeakPassword; return }
        // Copy the form now: [reset] empties the fields as soon as the account exists and this screen leaves,
        // which is before the name is stored below.
        val signUp = mode == Mode.SignUp
        val typedName = name
        val typedEmail = email
        val typedPassword = password
        launchAction {
            if (signUp) {
                auth.signUpWithEmail(typedName, typedEmail, typedPassword)
                auth.currentUser?.let { user ->
                    runCatching { users.setNameIfBlank(user.uid, typedName) }
                }
            } else {
                auth.signInWithEmail(typedEmail, typedPassword)
            }
        }
    }

    fun forgotPassword() {
        if (email.isBlank()) { error = AppError.InvalidEmail; return }
        launchAction {
            auth.sendPasswordReset(email)
            resetSent = true
        }
    }

    // On success there's nothing to do here: AppViewModel sees the new auth state and moves on.
    private fun launchAction(block: suspend () -> Unit) {
        if (loading) return
        loading = true
        error = null
        resetSent = false
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
