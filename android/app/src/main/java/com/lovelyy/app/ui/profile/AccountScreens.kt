package com.lovelyy.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.cat.CatViewModel
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.ConfirmDialog
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.history.Danger
import com.lovelyy.app.ui.history.DangerTint
import com.lovelyy.app.ui.history.InnerColumn
import com.lovelyy.app.ui.history.MiniAvatar
import com.lovelyy.app.ui.history.Muted
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAIL_D = "M6 6h12a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z M4 8l8 6 8-6"
private const val KEY_D = "M14.5 9.5a4 4 0 1 1-8 0a4 4 0 1 1 8 0z M13.5 12.5L20 19M17 16l2-2M18.5 17.5l1.5-1.5"
private const val LOGOUT_D = "M14 5h3a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-3M10 8l-4 4 4 4M6 12h9"
private const val TRASH_D = "M5 7h14M10 7V5h4v2M7 7l1 12a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2l1-12M10 11v6M14 11v6"
private const val CHEVRON_D = "M9 5l7 7-7 7"
private const val HEART_BREAK_D = "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z M12 7.5l-1.5 4 3 1.5-1.5 4"
private const val CAT_D = "M5 9l1-5 4 3h4l4-3 1 5v5a7 7 0 0 1-14 0z M9.5 13h.01M14.5 13h.01"
private const val BOX_D = "M4 8l8-4 8 4v8l-8 4-8-4z M4 8l8 4 8-4M12 12v8"
private const val BELL_D = "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z M10 20.5a2 2 0 0 0 4 0"
private const val USER_D = "M15.5 8a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z M5 20a7 7 0 0 1 14 0"
private val SectionGrey = Color(0xFFF6EEE8)

/** The confirm word for unpairing. Typed in capitals; compared without case so the keyboard can't trip it. */
private const val CONFIRM_WORD = "KALDIR"

/**
 * Hesap (approved board 33): how I sign in, my e-mail, password reset (e-mail accounts only), sign out,
 * and deleting the account (unpairs first, so the shared data is deleted for both).
 */
@Composable
fun AccountScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = app.authRepository.currentUser
    val google = remember { user?.providerData?.any { it.providerId == "google.com" } == true }
    val email = user?.email.orEmpty()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val days = couple?.startDate?.toDate()?.time?.let { DayCounter.together(it, System.currentTimeMillis()).days }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val resetSent = stringResource(R.string.acc_reset_sent, email)
    val resetFailed = stringResource(R.string.acc_reset_failed)
    val leave: LeaveViewModel = viewModel { LeaveViewModel(app) }
    val busy by leave.busy.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LeaveEffects(leave)

    InnerColumn(spacing = 10) {
        CenteredTopBar(stringResource(R.string.pf_account), onBack)
        // Peach header: me, and who I'm paired with.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PeachSoft).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MiniAvatar(me.displayName, Peach, 56, Modifier.border(3.dp, Color.White, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(me.displayName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                Text(
                    if (days != null) stringResource(R.string.acc_paired_days, partnerName, days) else stringResource(R.string.acc_paired, partnerName),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubText,
                )
            }
        }

        Section(stringResource(R.string.acc_login))
        Card {
            InfoRow(if (google) null else MAIL_D, stringResource(R.string.acc_method), stringResource(if (google) R.string.acc_method_google else R.string.acc_method_email), googleBadge = google)
            InfoRow(MAIL_D, stringResource(R.string.acc_email), email.ifBlank { "–" })
            if (google) {
                ActionRow(KEY_D, stringResource(R.string.acc_password), stringResource(R.string.acc_password_google), enabled = false) { }
            } else {
                ActionRow(KEY_D, stringResource(R.string.acc_password), null) { confirmReset = true }
            }
        }

        Section(stringResource(R.string.acc_session))
        Card { ActionRow(LOGOUT_D, stringResource(R.string.sign_out), stringResource(R.string.acc_sign_out_sub)) { confirmSignOut = true } }

        Section(stringResource(R.string.acc_danger))
        Card {
            ActionRow(TRASH_D, stringResource(R.string.acc_delete), stringResource(R.string.acc_delete_sub, partnerName), color = Danger, enabled = !busy) {
                confirmDelete = true
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.acc_delete_title),
            body = stringResource(if (me.coupleId != null) R.string.acc_delete_body_paired else R.string.acc_delete_body, partnerName),
            confirm = stringResource(R.string.acc_delete_yes),
            onConfirm = { confirmDelete = false; leave.deleteAccount(me) },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.sign_out_title),
            body = stringResource(R.string.sign_out_body),
            confirm = stringResource(R.string.sign_out),
            onConfirm = { confirmSignOut = false; scope.launch { app.authRepository.signOut(context) } },
            onDismiss = { confirmSignOut = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.acc_reset_title),
            body = stringResource(R.string.acc_reset_body, email),
            confirm = stringResource(R.string.acc_reset_send),
            onConfirm = {
                confirmReset = false
                scope.launch {
                    try {
                        app.authRepository.sendPasswordReset(email)
                        com.lovelyy.app.telemetry.Telemetry.track(com.lovelyy.app.analytics.AnalyticsEvent.PasswordResetSent)
                        Toast.makeText(context, resetSent, Toast.LENGTH_LONG).show()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Password reset mail failed")
                        Toast.makeText(context, resetFailed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun Section(text: String) {
    Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(Color.White).padding(horizontal = 16.dp, vertical = 6.dp), content = content)
}

@Composable
private fun InfoRow(icon: String?, label: String, value: String, googleBadge: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (googleBadge) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(SectionGrey), contentAlignment = Alignment.Center) {
                Text("G", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown)
            }
        } else if (icon != null) {
            SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
        }
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, modifier = Modifier.weight(1f))
        Text(value, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Muted, maxLines = 1)
    }
}

@Composable
private fun ActionRow(icon: String, label: String, sub: String?, color: Color = Brown, enabled: Boolean = true, onClick: () -> Unit) {
    val tint = if (enabled) color else Muted
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 6.dp)
            .semantics { role = Role.Button }
            .then(if (enabled) Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SvgStrokeIcon(icon, tint, Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = tint)
            if (sub != null) Text(sub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = Muted)
        }
        if (enabled) SvgStrokeIcon(CHEVRON_D, Muted, Modifier.size(16.dp))
    }
}

/**
 * Eşleşmeyi kaldır (approved board 34): what unpairing does, then type KALDIR to enable the red button.
 * Unpairing deletes the shared data for both partners (LeaveRepository).
 */
@Composable
fun UnpairScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val cat: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val catUi by cat.ui.collectAsStateWithLifecycle()
    val catName = catUi?.doc?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cat_generic)
    val streak = catUi?.doc?.streak?.best ?: 0
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var typed by rememberSaveable { mutableStateOf("") }
    val ok = typed.trim().equals(CONFIRM_WORD, ignoreCase = true)
    val leave: LeaveViewModel = viewModel { LeaveViewModel(app) }
    val busy by leave.busy.collectAsStateWithLifecycle()
    LeaveEffects(leave)

    InnerColumn(spacing = 14) {
        CenteredTopBar(stringResource(R.string.pf_unpair), onBack)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniAvatar(me.displayName, Peach, 52)
                SvgStrokeIcon(HEART_BREAK_D, Danger, Modifier.size(26.dp))
                MiniAvatar(partnerName, Mint, 52)
            }
            Text(
                stringResource(R.string.un_title, partnerName), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                lineHeight = 26.sp, color = Brown, textAlign = TextAlign.Center,
            )
        }

        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(Color.White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.un_what), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            Consequence(CAT_D, stringResource(R.string.un_cat, catName), stringResource(R.string.un_cat_sub, catName, streak))
            Consequence(BOX_D, stringResource(R.string.un_data), stringResource(R.string.un_data_sub))
            Consequence(BELL_D, stringResource(R.string.un_partner, partnerName), stringResource(R.string.un_partner_sub, TurkishSuffix.dative(partnerName)))
            Consequence(USER_D, stringResource(R.string.un_account), stringResource(R.string.un_account_sub))
        }

        Text(
            buildAnnotatedString {
                append(stringResource(R.string.un_type_before))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Danger)) { append(CONFIRM_WORD) }
                append(stringResource(R.string.un_type_after))
            },
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SubText, modifier = Modifier.padding(start = 4.dp),
        )
        val fieldShape = RoundedCornerShape(18.dp)
        BasicTextField(
            value = typed,
            onValueChange = { typed = it.take(10) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, letterSpacing = 2.sp),
            cursorBrush = SolidColor(Brown),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(fieldShape).background(Color.White)
                .border(2.dp, if (ok) Danger else Color(0xFFEADFD6), fieldShape)
                .semantics { contentDescription = context.getString(R.string.un_field) },
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    if (typed.isEmpty()) Text(CONFIRM_WORD, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Muted.copy(alpha = 0.6f), letterSpacing = 2.sp)
                    inner()
                }
            },
        )

        val btnShape = RoundedCornerShape(28.dp)
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(btnShape).background(if (ok) Danger else DangerTint)
                .semantics { role = Role.Button }
                .bounceClick(enabled = ok && !busy) { leave.unpair(me) },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.un_button), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (ok) Color.White else Danger.copy(alpha = 0.6f))
            }
        }
        Text(
            stringResource(R.string.un_keep), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 44.dp).bounceClick(onClick = onBack).padding(12.dp),
        )
    }
}

@Composable
private fun Consequence(icon: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(DangerTint), contentAlignment = Alignment.Center) {
            SvgStrokeIcon(icon, Danger, Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
            Text(body, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = SubText)
        }
    }
}

/** Friendly messages for what can go wrong while leaving. */
@Composable
private fun LeaveEffects(leave: LeaveViewModel) {
    val context = LocalContext.current
    val failed = stringResource(R.string.leave_failed)
    val again = stringResource(R.string.acc_delete_sign_in_again)
    LaunchedEffect(leave) {
        leave.oneTimeEvents.collect { e ->
            Toast.makeText(context, if (e == LeaveEvent.SignInAgain) again else failed, Toast.LENGTH_LONG).show()
        }
    }
}
