package com.lovelyy.app.ui.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.data.AppError
import com.lovelyy.app.data.AppException
import com.lovelyy.app.data.AuthRepository
import com.lovelyy.app.data.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    enum class Mode { Choose, SignIn, SignUp }

    var mode by mutableStateOf(Mode.Choose)
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<AppError?>(null)
        private set

    fun switchMode(newMode: Mode) {
        mode = newMode
        error = null
    }

    fun google(activityContext: Context) = launchAction { auth.signInWithGoogle(activityContext) }

    fun submitEmail() {
        if (email.isBlank()) { error = AppError.InvalidEmail; return }
        if (password.length < 6) { error = AppError.WeakPassword; return }
        launchAction {
            if (mode == Mode.SignUp) {
                auth.signUpWithEmail(name, email, password)
                auth.currentUser?.let { user ->
                    runCatching { users.setNameIfBlank(user.uid, name) }
                }
            } else {
                auth.signInWithEmail(email, password)
            }
        }
    }

    // On success there's nothing to do here: AppViewModel sees the new auth state and moves on.
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
