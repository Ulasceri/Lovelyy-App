package com.lovelyy.app.ui.heartbeat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.SignalCenter
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.signals.messageRes
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CLOCK_D
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val RingTrack = Color(0xFFF3EAE3)
private val NudgeTint = Color(0xFFD6F0E8)

/** Look of each feeling in the approved heartbeat design: its color and its 24×24 icon. */
data class EmotionLook(val color: Color, val icon: String)

/** The five feelings in the picker, in the design's order. */
val PickerEmotions = listOf(Emotion.Ozlem, Emotion.Sevgi, Emotion.Ask, Emotion.Sarilma, Emotion.Opucuk)

fun Emotion.look(): EmotionLook = when (this) {
    Emotion.Ozlem -> EmotionLook(Color(0xFFC9D4F5), "M11 19s-7-4.35-7-10a4 4 0 0 1 7-2.65A4 4 0 0 1 18 9c0 5.65-7 10-7 10z M19.5 15s2 2.4 2 3.9a2 2 0 0 1-4 0c0-1.5 2-3.9 2-3.9z")
    Emotion.Sevgi, Emotion.Huzur -> EmotionLook(Peach, HEART_D)
    Emotion.Ask -> EmotionLook(Color(0xFFFF9E9E), "M10 21s-7-4.35-7-10a4 4 0 0 1 7-2.65A4 4 0 0 1 17 11c0 5.65-7 10-7 10z M18.5 8.5s-3-1.9-3-4.3a1.7 1.7 0 0 1 3-1.1 1.7 1.7 0 0 1 3 1.1c0 2.4-3 4.3-3 4.3z")
    Emotion.Sarilma -> EmotionLook(Mint, "M8 4a3 3 0 1 1 0 6 3 3 0 0 1 0-6z M16 4a3 3 0 1 1 0 6 3 3 0 0 1 0-6z M3 20c0-5 2.5-8 5.5-8 1.4 0 2.6.6 3.5 1.6.9-1 2.1-1.6 3.5-1.6 3 0 5.5 3 5.5 8z")
    Emotion.Opucuk -> EmotionLook(Color(0xFFF8B7C8), "M3 12c2-3 4.5-5 6-5 1.2 0 2 .8 3 1.5 1-.7 1.8-1.5 3-1.5 1.5 0 4 2 6 5-2 4-5.5 6-9 6s-7-2-9-6z")
}

private const val NUDGE_D = "M9 13V5.5a1.5 1.5 0 0 1 3 0V11l4.6 1.2a2 2 0 0 1 1.4 2.3l-.7 3.5a3 3 0 0 1-3 2.5H11a3 3 0 0 1-2.5-1.3L5.5 15a1.5 1.5 0 0 1 2.3-1.9z M15 4.5a5 5 0 0 1 1.5 3.5M5.5 8a5 5 0 0 1 1.5-3.5"
private const val HOLD_MS = 1500

class HeartbeatViewModel(private val app: LovelyyApplication) : ViewModel() {
    suspend fun sendHeartbeat(me: UserProfile, partnerUid: String, emotion: Emotion): Boolean {
        val coupleId = me.coupleId ?: return false
        return runCatching { app.signalRepository.send(coupleId, me, partnerUid, SignalKind.Heartbeat, emotion) }
            .onSuccess { Telemetry.track(AnalyticsEvent.HeartbeatSent(emotion.key)) }
            .onFailure { Timber.w(it, "Sending a heartbeat failed") }
            .isSuccess
    }

    suspend fun nudge(me: UserProfile, partnerUid: String): Boolean {
        val coupleId = me.coupleId ?: return false
        return runCatching { app.signalRepository.send(coupleId, me, partnerUid, SignalKind.Nudge) }
            .onSuccess { Telemetry.track(AnalyticsEvent.NudgeSent) }
            .onFailure { Timber.w(it, "Sending a nudge failed") }
            .isSuccess
    }
}

private enum class Hold { Idle, Holding, Sending, Sent, Failed }

/** Kalp atışı + dürtme (approved design 6). */
@Composable
fun HeartbeatScreen(me: UserProfile, partner: UserProfile?, initial: Emotion?, onHistory: () -> Unit) {
    val app = lovelyyApp()
    val vm: HeartbeatViewModel = viewModel { HeartbeatViewModel(app) }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var emotion by rememberSaveable { mutableStateOf(initial?.takeIf { it in PickerEmotions } ?: Emotion.Ozlem) }
    var hold by remember { mutableStateOf(Hold.Idle) }
    val progress = remember { Animatable(0f) }
    val recent by app.signalCenter.recent.collectAsState()
    val lastIn = recent.firstOrNull { it.to == me.uid && it.kind == SignalKind.Heartbeat }
    val look = emotion.look()
    val color by animateColorAsState(look.color, label = "emotion")

    LaunchedEffect(hold) {
        if (hold == Hold.Sent || hold == Hold.Failed) {
            delay(2400)
            hold = Hold.Idle
            progress.snapTo(0f)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header: both of you, who to send to, the last heart you got.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                Initial(me.displayName, Peach)
                Initial(partnerName, Mint)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.hb2_title, TurkishSuffix.dative(partnerName)),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (lastIn != null) stringResource(
                        R.string.hb2_last, lastIn.fromName.ifBlank { partnerName }, timeAgo(lastIn.at),
                        stringResource((lastIn.emotion ?: Emotion.Sevgi).labelRes()).lowercase(TR),
                    ) else stringResource(R.string.hb2_none),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft, maxLines = 2,
                )
            }
            RoundSvgButton(CLOCK_D, stringResource(R.string.hb2_history), onHistory)
        }

        // What the partner's notification will say.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.hb2_preview_label, TurkishSuffix.genitive(partnerName)),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft, modifier = Modifier.padding(start = 4.dp),
            )
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) {
                    SvgIcon(look.icon, Brown, Modifier.size(22.dp))
                }
                Column {
                    Text(stringResource(R.string.hb2_preview_app), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
                    Text(stringResource(emotion.messageRes(), me.displayName), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                }
            }
        }

        // The big button: hold until the ring is full.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p",
            )
            val beat by rememberInfiniteTransition(label = "beat").animateFloat(
                1f, 1f,
                infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; 1.08f at 270; 1f at 540; 1.05f at 810; 1f at 1080 }),
                label = "b",
            )
            var holdJob by remember { mutableStateOf<Job?>(null) }
            val holdLabel = stringResource(R.string.hb2_hold_cd)
            Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                listOf(pulse, (pulse + 0.5f) % 1f).forEach { p ->
                    Box(
                        Modifier.size(180.dp).scale(0.8f + 0.5f * p).alpha(0.55f * (1f - p)).clip(CircleShape).background(color),
                    )
                }
                Canvas(Modifier.size(212.dp).rotate(-90f)) {
                    val w = 8.dp.toPx()
                    val r = 98.dp.toPx()
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(RingTrack, r, c, style = Stroke(w))
                    drawArc(
                        Brown, 0f, 360f * progress.value, false,
                        topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2), style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
                Box(
                    Modifier
                        .size(172.dp)
                        .scale(beat)
                        .softShadow(CircleShape, 16.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(6.dp, Cream, CircleShape)
                        .semantics { contentDescription = holdLabel; role = Role.Button }
                        .pointerInput(emotion, partner) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val p = partner
                                if ((hold != Hold.Idle && hold != Hold.Holding) || p == null) return@awaitEachGesture
                                hold = Hold.Holding
                                holdJob = scope.launch {
                                    val ticker = launch {
                                        // A soft "ba-dum" under the finger while holding.
                                        while (isActive) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            delay(120)
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            delay(330)
                                        }
                                    }
                                    progress.animateTo(1f, tween(((1f - progress.value) * HOLD_MS).toInt(), easing = LinearEasing))
                                    ticker.cancel()
                                    hold = Hold.Sending
                                    val ok = vm.sendHeartbeat(me, p.uid, emotion)
                                    if (ok) SignalCenter.vibrate(context, SignalKind.Heartbeat)
                                    hold = if (ok) Hold.Sent else Hold.Failed
                                }
                                waitForUpOrCancellation()
                                if (hold == Hold.Holding) {
                                    holdJob?.cancel()
                                    hold = Hold.Idle
                                    scope.launch { progress.animateTo(0f, tween(250)) }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SvgIcon(look.icon, Brown, Modifier.size(84.dp))
                }
            }
            Text(
                when {
                    partner == null -> stringResource(R.string.hb_no_partner)
                    hold == Hold.Sent -> stringResource(R.string.hb2_hint_sent, TurkishSuffix.genitive(partnerName))
                    hold == Hold.Failed -> stringResource(R.string.hb2_hint_failed)
                    hold == Hold.Holding || hold == Hold.Sending -> stringResource(R.string.hb2_hint_holding)
                    else -> stringResource(R.string.hb2_hint_idle, TurkishSuffix.dative(partnerName))
                },
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, textAlign = TextAlign.Center,
            )
        }

        // Pick a feeling.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PickerEmotions.forEach { e ->
                val sel = e == emotion
                Column(
                    Modifier
                        .weight(1f)
                        .semantics { selected = sel; role = Role.RadioButton }
                        .bounceClick(enabled = hold == Hold.Idle) { emotion = e }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .then(if (sel) Modifier.border(3.dp, Brown, CircleShape) else Modifier)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(e.look().color), contentAlignment = Alignment.Center) {
                            SvgIcon(e.look().icon, Brown, Modifier.size(26.dp))
                        }
                    }
                    Text(stringResource(e.labelRes()), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1)
                }
            }
        }

        NudgeRow(me, partner, vm)
    }
}

@Composable
private fun NudgeRow(me: UserProfile, partner: UserProfile?, vm: HeartbeatViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(0) } // 0 ready, 1 sending, 2 sent
    LaunchedEffect(state) { if (state == 2) { delay(2000); state = 0 } }
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(NudgeTint), contentAlignment = Alignment.Center) {
            SvgStrokeIcon(NUDGE_D, Brown, Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.hb2_nudge_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
            Text(stringResource(R.string.hb2_nudge_body, me.displayName), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
        }
        Box(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Mint)
                .semantics { role = Role.Button }
                .bounceClick(enabled = state == 0 && partner != null) {
                    val p = partner ?: return@bounceClick
                    state = 1
                    scope.launch {
                        val ok = vm.nudge(me, p.uid)
                        if (ok) SignalCenter.vibrate(context, SignalKind.Nudge)
                        state = if (ok) 2 else 0
                    }
                }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (state == 2) R.string.hb2_nudge_done else R.string.hb2_nudge_action),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
            )
        }
    }
}

@Composable
private fun Initial(name: String, color: Color) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase(TR) ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown)
    }
}

/** "az önce", "5 dakika önce", "2 saat önce"... */
@Composable
fun timeAgo(at: Long): String {
    val minutes = ((System.currentTimeMillis() - at) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.time_ago_now)
        minutes < 60 -> stringResource(R.string.time_ago_minutes, minutes.toInt())
        minutes < 24 * 60 -> stringResource(R.string.time_ago_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.time_ago_days, (minutes / (24 * 60)).toInt())
    }
}
