package com.lovelyy.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.BeatingHeart
import com.lovelyy.app.ui.components.ErrorText
import com.lovelyy.app.ui.components.LovelyyTextField
import com.lovelyy.app.ui.components.PrimaryButton
import com.lovelyy.app.ui.components.SecondaryButton
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.LovelyyTheme

@Composable
fun LoginScreen() {
    val app = lovelyyApp()
    val vm: LoginViewModel = viewModel { LoginViewModel(app.authRepository, app.userRepository) }
    val context = LocalContext.current

    BackHandler(enabled = vm.mode != LoginViewModel.Mode.Choose) { vm.switchMode(LoginViewModel.Mode.Choose) }

    LoginContent(
        mode = vm.mode,
        name = vm.name, onName = { vm.name = it },
        email = vm.email, onEmail = { vm.email = it },
        password = vm.password, onPassword = { vm.password = it },
        loading = vm.loading,
        errorSlot = { ErrorText(vm.error) },
        onGoogle = { vm.google(context) },
        onEmailChosen = { vm.switchMode(LoginViewModel.Mode.SignIn) },
        onSwitch = {
            vm.switchMode(
                if (vm.mode == LoginViewModel.Mode.SignIn) LoginViewModel.Mode.SignUp else LoginViewModel.Mode.SignIn
            )
        },
        onBack = { vm.switchMode(LoginViewModel.Mode.Choose) },
        onSubmit = vm::submitEmail,
    )
}

@Composable
private fun LoginContent(
    mode: LoginViewModel.Mode,
    name: String, onName: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    loading: Boolean,
    errorSlot: @Composable () -> Unit,
    onGoogle: () -> Unit,
    onEmailChosen: () -> Unit,
    onSwitch: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    // Scrollable (keyboard can cover the form) but at least screen-tall, so the
    // brand sits on top and the actions sit in the lower half (one-hand use).
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(), // includes the keyboard (IME) inset
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(56.dp))
            BeatingHeart(size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(32.dp))
        }

        AnimatedContent(targetState = mode, label = "loginMode") { m ->
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (m == LoginViewModel.Mode.Choose) {
                    Text(
                        stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    errorSlot()
                    PrimaryButton(stringResource(R.string.login_google), onGoogle, loading = loading)
                    SecondaryButton(
                        text = stringResource(R.string.login_email),
                        onClick = onEmailChosen,
                        enabled = !loading,
                        leading = {
                            Icon(Icons.Filled.AccountCircle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                        },
                    )
                } else {
                    val signUp = m == LoginViewModel.Mode.SignUp
                    Text(
                        stringResource(if (signUp) R.string.login_title_signup else R.string.login_title_signin),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (signUp) {
                        LovelyyTextField(name, onName, stringResource(R.string.field_name))
                    }
                    LovelyyTextField(
                        email, onEmail, stringResource(R.string.field_email),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    LovelyyTextField(
                        password, onPassword, stringResource(R.string.field_password),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    errorSlot()
                    PrimaryButton(
                        stringResource(if (signUp) R.string.action_signup else R.string.action_signin),
                        onSubmit,
                        enabled = !signUp || name.isNotBlank(),
                        loading = loading,
                    )
                    TextButton(onClick = onSwitch) {
                        Text(stringResource(if (signUp) R.string.switch_to_signin else R.string.switch_to_signup))
                    }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                }
            }
        }
    }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F2)
@Composable
private fun LoginPreview() {
    LovelyyTheme {
        LoginContent(
            mode = LoginViewModel.Mode.Choose,
            name = "", onName = {}, email = "", onEmail = {}, password = "", onPassword = {},
            loading = false, errorSlot = {}, onGoogle = {}, onEmailChosen = {}, onSwitch = {}, onBack = {}, onSubmit = {},
        )
    }
}
