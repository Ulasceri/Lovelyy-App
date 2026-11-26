package com.lovelyy.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.BackButton
import com.lovelyy.app.ui.components.DesignField
import com.lovelyy.app.ui.components.ErrorText
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.OutlineButton
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.onboarding.IntroScreen
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.LovelyyTheme
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.Quicksand

@Composable
fun LoginScreen() {
    val app = lovelyyApp()
    val vm: LoginViewModel = viewModel { LoginViewModel(app.authRepository, app.userRepository) }
    val context = LocalContext.current

    // The intro handles its own back presses (page by page), so this one is only for the forms.
    BackHandler(enabled = vm.mode == LoginViewModel.Mode.SignIn || vm.mode == LoginViewModel.Mode.SignUp) {
        vm.switchMode(LoginViewModel.Mode.Welcome)
    }

    // 0 = Karşılama, 1 = tanıtım, 2 = giriş/kayıt formu: later steps slide in from the right.
    AnimatedContent(
        targetState = when (vm.mode) {
            LoginViewModel.Mode.Welcome -> 0
            LoginViewModel.Mode.Intro -> 1
            else -> 2
        },
        transitionSpec = {
            if (targetState < initialState) (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
            else (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
        },
        label = "welcome",
    ) { step ->
        if (step == 0) {
            WelcomeContent(
                onStart = vm::startIntro,
                onHaveAccount = { vm.switchMode(LoginViewModel.Mode.SignIn) },
            )
        } else if (step == 1) {
            IntroScreen(
                onBack = { vm.switchMode(LoginViewModel.Mode.Welcome) },
                onDone = vm::finishIntro,
            )
        } else {
            LoginContent(
                signUp = vm.mode == LoginViewModel.Mode.SignUp,
                name = vm.name, onName = { vm.name = it; vm.clearError() },
                email = vm.email, onEmail = { vm.email = it; vm.clearError() },
                password = vm.password, onPassword = { vm.password = it; vm.clearError() },
                loading = vm.loading,
                resetSent = vm.resetSent,
                errorSlot = { ErrorText(vm.error, Modifier.fillMaxWidth()) },
                onGoogle = { vm.google(context) },
                onSwitch = {
                    vm.switchMode(if (vm.mode == LoginViewModel.Mode.SignIn) LoginViewModel.Mode.SignUp else LoginViewModel.Mode.SignIn)
                },
                onForgot = vm::forgotPassword,
                onBack = { vm.switchMode(LoginViewModel.Mode.Welcome) },
                onSubmit = vm::submitEmail,
            )
        }
    }
}

// ---------- Karşılama ----------

@Composable
private fun WelcomeContent(onStart: () -> Unit, onHaveAccount: () -> Unit) {
    val float = rememberInfiniteTransition(label = "float")
    val dy by float.animateFloat(0f, -8f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Soft background blobs.
        Box(
            Modifier
                .offset(x = (-15).dp, y = 70.dp)
                .size(maxWidth + 30.dp)
                .clip(CircleShape)
                .background(PeachSoft)
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 40.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(MintSoft)
        )

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Rounded.Favorite, null, tint = Peach, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_name), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Brown)
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(272.dp)
                        .graphicsLayer { translationY = dy * density }
                        .softShadow(CircleShape, 18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8EDD7))
                ) {
                    Image(
                        painterResource(R.drawable.cat_base),
                        contentDescription = stringResource(R.string.welcome_cat_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                RisingHeart(Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 56.dp), 22.dp, Peach, 0)
                RisingHeart(Modifier.align(Alignment.TopStart).padding(top = 110.dp, start = 50.dp), 16.dp, Mint, 1100)
                RisingHeart(Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 90.dp), 14.dp, Peach, 2200)
            }

            Text(
                stringResource(R.string.welcome_title),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp,
                color = Brown, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.welcome_body),
                fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp,
                color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(28.dp))
            PeachButton(stringResource(R.string.welcome_start), onStart)
            Spacer(Modifier.height(12.dp))
            GhostButton("", onHaveAccount) {
                Row {
                    Text(stringResource(R.string.welcome_have_account), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brown)
                    Text(
                        stringResource(R.string.action_signin),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}

/** A little heart that floats up and fades, forever (the design's `lv-rise`). */
@Composable
private fun RisingHeart(modifier: Modifier, size: Dp, color: Color, delayMs: Int) {
    val t by rememberInfiniteTransition(label = "rise").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), initialStartOffset = StartOffset(delayMs)),
        label = "t",
    )
    Icon(
        Icons.Rounded.Favorite, null, tint = color,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = -40.dp.toPx() * t
                val s = 0.8f + 0.3f * t
                scaleX = s; scaleY = s
                alpha = if (t < 0.2f) t / 0.2f else 1f - (t - 0.2f) / 0.8f
            },
    )
}

// ---------- Giriş / Kayıt ----------

@Composable
private fun LoginContent(
    signUp: Boolean,
    name: String, onName: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    loading: Boolean,
    resetSent: Boolean,
    errorSlot: @Composable () -> Unit,
    onGoogle: () -> Unit,
    onSwitch: () -> Unit,
    onForgot: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    var showPassword by remember { mutableStateOf(false) }
    // Name hint appears once the user has been in the name field and left it still empty.
    var nameFocused by remember { mutableStateOf(false) }
    var nameLeft by remember { mutableStateOf(false) }
    // Scrollable (the keyboard can cover the form) but at least screen-tall, so the
    // greeting sits on top and the actions sit in the lower half (one-hand use).
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
        ) {
            BackButton(onBack)

            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(if (signUp) 96.dp else 120.dp)
                        .softShadow(CircleShape, 12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8EDD7))
                ) {
                    Image(painterResource(R.drawable.cat_base), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(if (signUp) R.string.login_title_signup else R.string.login_title_signin),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Brown,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (signUp) R.string.login_body_signup else R.string.login_body_signin),
                    fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp,
                    color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 290.dp),
                )
            }

            Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PeachButton(
                    stringResource(R.string.login_google), onGoogle,
                    loading = loading,
                    leading = {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(CreamCard), contentAlignment = Alignment.Center) {
                            Image(painterResource(R.drawable.ic_google), null, Modifier.size(18.dp))
                        }
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                    Box(Modifier.weight(1f).height(1.5.dp).background(Outline))
                    Text(
                        stringResource(R.string.login_or_email),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF8A7A73),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Box(Modifier.weight(1f).height(1.5.dp).background(Outline))
                }
                if (signUp) {
                    DesignField(
                        name, onName, stringResource(R.string.field_name_ph),
                        modifier = Modifier.onFocusChanged {
                            if (nameFocused && !it.isFocused) nameLeft = true
                            nameFocused = it.isFocused
                        },
                        leadingIcon = Icons.Rounded.PersonOutline,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    if (nameLeft && name.isBlank()) {
                        Text(
                            stringResource(R.string.field_name_hint),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                DesignField(
                    email, onEmail, stringResource(R.string.field_email_ph),
                    leadingIcon = Icons.Rounded.MailOutline,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                DesignField(
                    password, onPassword, stringResource(R.string.field_password_ph),
                    leadingIcon = Icons.Rounded.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!loading && (!signUp || name.isNotBlank())) onSubmit() }),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).bounceClick { showPassword = !showPassword },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                stringResource(R.string.login_show_password),
                                tint = Color(0xFF8A7A73), modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
                if (!signUp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            stringResource(if (resetSent) R.string.login_reset_sent else R.string.login_forgot),
                            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = if (resetSent) MintInk else BrownSoft,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .bounceClick(enabled = !resetSent, onClick = onForgot)
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                }
                errorSlot()
                OutlineButton(
                    stringResource(if (signUp) R.string.action_signup_email else R.string.action_signin_email),
                    onSubmit,
                    enabled = !signUp || name.isNotBlank(),
                    loading = loading,
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (signUp) R.string.switch_have_account else R.string.switch_no_account),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft,
                    )
                    Text(
                        stringResource(if (signUp) R.string.action_signin else R.string.action_signup),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(onClick = onSwitch).padding(6.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F2, widthDp = 390, heightDp = 844)
@Composable
private fun WelcomePreview() {
    LovelyyTheme { WelcomeContent({}, {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F2, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() {
    LovelyyTheme {
        LoginContent(
            signUp = false,
            name = "", onName = {}, email = "", onEmail = {}, password = "", onPassword = {},
            loading = false, resetSent = false, errorSlot = {}, onGoogle = {}, onSwitch = {}, onForgot = {}, onBack = {}, onSubmit = {},
        )
    }
}
