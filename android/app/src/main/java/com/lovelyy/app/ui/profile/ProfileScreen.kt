package com.lovelyy.app.ui.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.displayCatName
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.cat.CatViewModel
import com.lovelyy.app.ui.cat.NameCatDialog
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.home.formatDate
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

private val Butter = Color(0xFFFFE2A8)
private val MintCell = Color(0xFFD6F0E8)
private val CellLabel = Color(0xFF5F524C)
private val Danger = Color(0xFFA8401C)

private const val BELL_D = "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z M10 20a2 2 0 0 0 4 0"
private const val PENCIL_D = "M4 20l4-1 11-11-3-3L5 16z"
private const val SMILE_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z M9 10v1M15 10v1M8.5 14.5c1 1.5 2.2 2 3.5 2s2.5-.5 3.5-2"
private const val BUBBLE_D = "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7l-5 4v-4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
private const val PIN_D = "M12 21s-7-6-7-11a7 7 0 0 1 14 0c0 5-7 11-7 11z M14.5 10a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0z"
private const val CLOCK_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z M12 7.5V12l3 2"
private const val GRID_D = "M6 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M15 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M6 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z M15 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z"
private const val PEOPLE_D = "M12.5 8a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z M19.5 9a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0z M3 20a6 6 0 0 1 12 0M15 20a4 4 0 0 1 6-3.5"
private const val X_D = "M7 7l10 10M17 7L7 17"

/** Biz tab (approved design 10): the two of you, shortcuts and settings. */
@Composable
fun ProfileScreen(
    me: UserProfile,
    couple: Couple?,
    partner: UserProfile?,
    onMood: () -> Unit,
    onQuestion: () -> Unit,
    onDistance: () -> Unit,
    onCounter: () -> Unit,
    onWidgets: () -> Unit,
    onInbox: () -> Unit,
    onAccount: () -> Unit,
    onUnpair: () -> Unit,
) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val startMillis = couple?.startDate?.toDate()?.time

    val cat: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val catUi by cat.ui.collectAsStateWithLifecycle()
    var naming by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }

    // Things that change outside the app (system settings, launcher): re-read when we come back.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val notificationsOn = remember(resumes) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val widgets = remember(resumes) { com.lovelyy.app.widget.WidgetData.count(context) }
    val locations by remember(coupleId) { app.locationRepository.observe(coupleId).catch { } }.collectAsStateWithLifecycle(emptyMap())
    val myLocation = locations[me.uid]
    val locationValue = stringResource(
        when {
            myLocation == null -> R.string.pf_off
            myLocation.cityOnly -> R.string.pf_location_city
            else -> R.string.pf_location_precise
        },
    )
    val google = remember { app.authRepository.currentUser?.providerData?.any { it.providerId == "google.com" } == true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: small avatars, "Biz / Profil ve ayarlar", bell.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatars(me.displayName, partnerName, 42.dp, 14.dp, 16.sp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.pf_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                Text(stringResource(R.string.pf_subtitle), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
            }
            RoundSvgButton(BELL_D, stringResource(R.string.pf_bell), onInbox)
        }

        // The couple card.
        val cardShape = RoundedCornerShape(28.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .softShadow(cardShape, 12.dp)
                .clip(cardShape)
                .background(PeachSoft)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatars(me.displayName, partnerName, 60.dp, 18.dp, 24.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_us, me.displayName, partnerName),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (startMillis != null) {
                    Text(
                        stringResource(R.string.pf_since, TurkishSuffix.ablative(formatDate(startMillis))),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CellLabel,
                    )
                    Text(
                        pluralStringResource(R.plurals.pf_together, (DayCounter.together(startMillis, System.currentTimeMillis()).days).toInt(), DayCounter.together(startMillis, System.currentTimeMillis()).days),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                    )
                } else {
                    Text(
                        stringResource(R.string.home_set_date),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CellLabel,
                        modifier = Modifier.bounceClick(onClick = onCounter),
                    )
                }
            }
            RoundSvgButton(PENCIL_D, stringResource(R.string.pf_edit), { editingName = true })
        }

        // Shortcuts.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut(SMILE_D, Butter, stringResource(R.string.pf_mood), Modifier.weight(1f), onMood)
            Shortcut(BUBBLE_D, PeachSoft, stringResource(R.string.pf_question), Modifier.weight(1f), onQuestion)
            Shortcut(PIN_D, MintCell, stringResource(R.string.pf_distance), Modifier.weight(1f), onDistance)
            Shortcut(CLOCK_D, CreamCard, stringResource(R.string.pf_days), Modifier.weight(1f), onCounter)
        }

        // Settings.
        SettingsCard {
            SettingRow(BELL_D, stringResource(R.string.pf_notifications), stringResource(if (notificationsOn) R.string.pf_on else R.string.pf_off)) {
                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                runCatching { context.startActivity(i) }
            }
            SettingRow(PIN_D, stringResource(R.string.pf_location), locationValue, onClick = onDistance)
            SettingRow(
                GRID_D, stringResource(R.string.pf_widgets),
                if (widgets > 0) stringResource(R.string.pf_widgets_count, widgets) else stringResource(R.string.pf_widgets_none),
                onClick = onWidgets,
            )
            SettingRow(PENCIL_D, stringResource(R.string.pf_cat_name), displayCatName(catUi?.doc?.name)) { naming = true }
            SettingRow(PEOPLE_D, stringResource(R.string.pf_account), stringResource(if (google) R.string.pf_account_google else R.string.pf_account_email), onClick = onAccount)
        }
        SettingsCard {
            SettingRow(X_D, stringResource(R.string.pf_unpair), "", color = Danger, onClick = onUnpair)
            SettingRow(LOGOUT_D, stringResource(R.string.sign_out), "") { confirmSignOut = true }
        }
    }

    if (confirmSignOut) {
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.sign_out_title),
            body = stringResource(R.string.sign_out_body),
            confirm = stringResource(R.string.sign_out),
            onConfirm = { confirmSignOut = false; scope.launch { app.authRepository.signOut(context) } },
            onDismiss = { confirmSignOut = false },
        )
    }

    if (naming) {
        NameCatDialog(
            initial = displayCatName(catUi?.doc?.name),
            canDismiss = true,
            onDismiss = { naming = false },
            onSave = { n ->
                naming = false
                cat.act(me, couple?.members.orEmpty(), CatActionType.Name, name = n)
            },
        )
    }
    if (editingName) {
        NameDialog(
            initial = me.displayName,
            onDismiss = { editingName = false },
            onSave = { n ->
                editingName = false
                scope.launch {
                    runCatching { app.userRepository.updateName(me.uid, n) }
                        .onSuccess { Telemetry.track(AnalyticsEvent.ProfileNameChanged) }
                        .onFailure { Timber.w(it, "Updating the name failed") }
                }
            },
        )
    }
}

@Composable
private fun Avatars(me: String, partner: String, size: Dp, overlap: Dp, fontSize: TextUnit) {
    Box(Modifier.width(size * 2 - overlap)) {
        Initial(me, Peach, size, fontSize, Modifier)
        Initial(partner, Mint, size, fontSize, Modifier.offset(x = size - overlap))
    }
}

@Composable
private fun Initial(name: String, color: Color, size: Dp, fontSize: TextUnit, modifier: Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = fontSize, color = Brown)
    }
}

@Composable
private fun Shortcut(icon: String, bg: Color, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.semantics { role = Role.Button }.bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(60.dp)
                .then(if (bg == CreamCard) Modifier.softShadow(CircleShape, 4.dp) else Modifier)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) { SvgStrokeIcon(icon, Brown, Modifier.size(24.dp)) }
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(horizontal = 16.dp, vertical = 6.dp)) {
        content()
    }
}

@Composable
private fun SettingRow(icon: String, label: String, value: String, color: Color = Brown, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button }
            .bounceClick(pressedScale = 0.98f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SvgStrokeIcon(icon, color, Modifier.size(20.dp))
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value.isNotEmpty()) {
            Text(value, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft, maxLines = 1)
        }
        SvgStrokeIcon(CHEVRON_D, BrownSoft, Modifier.size(16.dp))
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pf_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.pf_edit_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(com.lovelyy.app.data.UserRepository.MAX_NAME) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank(), colors = ButtonDefaults.textButtonColors(contentColor = Brown)) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Brown)) { Text(stringResource(R.string.action_cancel)) } },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.background,
    )
}
