package com.lovelyy.app.ui.heartbeat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.Signal
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.SignalCenter
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.signals.messageRes
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.home.NudgeCard
import com.lovelyy.app.ui.home.relativeTime
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val HOLD_MS = 1400

/** Kalp atışı: pick a feeling, press and hold the heart, the partner's phone beats with it. */
@Composable
fun HeartbeatScreen(me: UserProfile, partner: UserProfile?, initial: Emotion?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var emotion by rememberSaveable { mutableStateOf(initial ?: Emotion.Ozlem) }
    var status by remember { mutableStateOf(HoldStatus.Idle) }
    val progress = remember { Animatable(0f) }
    val recent by app.signalCenter.recent.collectAsState()

    fun send() {
        val p = partner ?: return
        val coupleId = me.coupleId ?: return
        status = HoldStatus.Sending
        scope.launch {
            val ok = runCatching { app.signalRepository.send(coupleId, me, p.uid, SignalKind.Heartbeat, emotion) }.isSuccess
            status = if (ok) HoldStatus.Sent else HoldStatus.Failed
            if (ok) SignalCenter.vibrate(context, SignalKind.Heartbeat)
            delay(2200)
            status = HoldStatus.Idle
            progress.snapTo(0f)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        ScreenTitle(stringResource(R.string.hb_title), onBack)
        Text(
            stringResource(R.string.hb_body, partnerName),
            fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft,
            modifier = Modifier.padding(top = 12.dp),
        )

        // The big heart to hold.
        val st = emotion.style()
        val bg by animateColorAsState(st.soft, label = "bg")
        var holdJob by remember { mutableStateOf<Job?>(null) }
        Box(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(28.dp))
                .background(bg)
                .pointerInput(emotion, partner) {
                    awaitEachGesture {
                        awaitFirstDown()
                        if (status != HoldStatus.Idle || partner == null) return@awaitEachGesture
                        status = HoldStatus.Holding
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
                            send()
                        }
                        val up = waitForUpOrCancellation()
                        if (status == HoldStatus.Holding) {
                            holdJob?.cancel()
                            status = HoldStatus.Idle
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        }
                        up?.consume()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val energy = if (status == HoldStatus.Holding) progress.value else if (status == HoldStatus.Sent) 1f else 0f
            EmotionVisual(emotion, Modifier.fillMaxSize(), energy = energy)
            // Progress ring around the heart.
            Canvas(Modifier.size(210.dp)) {
                val w = 8.dp.toPx()
                drawArc(
                    Color.White.copy(alpha = 0.7f), -90f, 360f, false,
                    topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                    style = Stroke(w),
                )
                drawArc(
                    st.deep, -90f, 360f * progress.value, false,
                    topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                    style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            AnimatedContent(
                targetState = status,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                label = "status",
            ) { s ->
                val text = when (s) {
                    HoldStatus.Idle -> if (partner == null) stringResource(R.string.hb_no_partner) else stringResource(R.string.hb_hold)
                    HoldStatus.Holding -> stringResource(R.string.hb_holding)
                    HoldStatus.Sending -> stringResource(R.string.hb_sending)
                    HoldStatus.Sent -> stringResource(R.string.hb_sent, partnerName)
                    HoldStatus.Failed -> stringResource(R.string.cat_error)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(CreamCard.copy(alpha = 0.9f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (s == HoldStatus.Idle) {
                        Icon(Icons.Rounded.TouchApp, null, tint = Brown, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
                }
            }
        }

        // Feelings.
        Text(
            stringResource(R.string.hb_pick),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        Emotion.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { e -> EmotionChip(e, e == emotion, Modifier.weight(1f)) { if (status == HoldStatus.Idle) emotion = e } }
            }
        }
        Text(
            stringResource(R.string.hb_preview, partnerName, stringResource(emotion.messageRes(), me.displayName)),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, color = BrownSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )

        NudgeCard(me, partner, Modifier.padding(top = 20.dp))

        // History.
        val mine = recent.filter { it.kind == SignalKind.Heartbeat || it.kind == SignalKind.Nudge }.take(6)
        if (mine.isNotEmpty()) {
            Text(
                stringResource(R.string.hb_recent),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
            SoftCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                mine.forEach { s -> HistoryRow(s, me.uid, partnerName) }
            }
        }
    }
}

private enum class HoldStatus { Idle, Holding, Sending, Sent, Failed }

@Composable
private fun EmotionChip(e: Emotion, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val st = e.style()
    val shape = RoundedCornerShape(20.dp)
    val bg by animateColorAsState(if (selected) st.soft else CreamCard, label = "chip")
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) st.color else Color(0xFFF0E6DE), shape)
            .bounceClick(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmotionVisual(e, Modifier.size(44.dp), heartScale = 0.5f)
        Text(stringResource(e.labelRes()), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun HistoryRow(s: Signal, myUid: String, partnerName: String) {
    val mine = s.from == myUid
    val what = when (s.kind) {
        SignalKind.Heartbeat -> stringResource((s.emotion ?: Emotion.Sevgi).labelRes())
        SignalKind.Nudge -> stringResource(R.string.hb_nudge_label)
        else -> ""
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val st = (s.emotion ?: Emotion.Sevgi).style()
        Box(Modifier.size(34.dp).clip(CircleShape).background(st.soft), contentAlignment = Alignment.Center) {
            EmotionVisual(s.emotion ?: Emotion.Sevgi, Modifier.size(30.dp), heartScale = 0.55f)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            if (mine) stringResource(R.string.hb_sent_row, what) else stringResource(R.string.hb_got_row, s.fromName.ifBlank { partnerName }, what),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Brown, modifier = Modifier.weight(1f),
        )
        Text(relativeTime(s.at), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
    }
}

/** Shown on top of everything when a heartbeat or nudge arrives while the app is open. */
@Composable
fun IncomingSignalOverlay(signal: Signal, onReply: (Emotion) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(signal.id) {
        delay(6500)
        onDismiss()
    }
    val emotion = signal.emotion ?: Emotion.Sevgi
    val st = emotion.style()
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6FFF8F2))
            .bounceClick(pressedScale = 1f, haptic = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(280.dp).clip(CircleShape).background(st.soft), contentAlignment = Alignment.Center) {
                EmotionVisual(emotion, Modifier.fillMaxSize(), energy = 0.6f)
            }
            Text(
                SignalCenter.message(context, signal),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, color = Brown,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp),
            )
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(st.color)
                    .bounceClick { onReply(emotion) }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.hb_reply), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
            }
        }
    }
}
