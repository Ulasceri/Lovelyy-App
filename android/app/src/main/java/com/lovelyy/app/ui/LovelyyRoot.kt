package com.lovelyy.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.ui.auth.LoginScreen
import com.lovelyy.app.ui.home.HomeScreen
import com.lovelyy.app.ui.onboarding.PairedFlow
import com.lovelyy.app.ui.pairing.PairingScreen
import com.lovelyy.app.ui.profile.NameScreen
import com.lovelyy.app.ui.setup.ErrorScreen
import com.lovelyy.app.ui.setup.FirebaseSetupScreen

@Composable
fun LovelyyRoot() {
    val app = LocalContext.current.applicationContext as LovelyyApplication
    val vm: AppViewModel = viewModel { AppViewModel(app) }
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Gentle cross-fade between big screens (micro-animation principle).
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "root",
        ) { s ->
            when (s) {
                AppState.Loading -> LoadingScreen()
                AppState.FirebaseMissing -> FirebaseSetupScreen()
                AppState.SignedOut -> LoginScreen()
                is AppState.NeedsName -> NameScreen(s.me)
                is AppState.Pairing -> PairingScreen(s.me)
                is AppState.JustPaired -> PairedFlow(
                    me = s.me,
                    coupleId = s.coupleId,
                    onNotificationAnswer = vm::notificationPrimerAnswered,
                    onLocationAnswer = vm::locationPrimerAnswered,
                    onDone = { vm.finishPairedFlow(s.me.uid) },
                )
                is AppState.Home -> HomeScreen(s.me, s.couple, s.partner)
                is AppState.Failed -> ErrorScreen(s.message)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Helper for screen ViewModels. */
@Composable
fun lovelyyApp(): LovelyyApplication = LocalContext.current.applicationContext as LovelyyApplication

