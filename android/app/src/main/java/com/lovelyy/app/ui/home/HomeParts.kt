package com.lovelyy.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.lovelyy.app.R
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.SignalCenter
import com.lovelyy.app.ui.components.IconCircle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** "Aklımdasın": one tap, the partner's phone buzzes softly with "X seni düşünüyor". */
@Composable
fun NudgeCard(me: UserProfile, partner: UserProfile?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var state by remember { mutableStateOf(0) } // 0 ready, 1 sending, 2 sent (cool-down)
    LaunchedEffect(state) {
        if (state == 2) { delay(NUDGE_COOLDOWN_MS); state = 0 }
    }
    SoftCard(modifier, padding = 16.dp) {
        IconCircle(Icons.Rounded.TouchApp, PeachTint, tint = Brown)
        Text(
            stringResource(R.string.nudge_title),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            stringResource(if (state == 2) R.string.nudge_sent else R.string.nudge_body, partnerName),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownSoft,
            modifier = Modifier.padding(top = 6.dp).then(if (compact) Modifier.height(54.dp) else Modifier),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (state == 0) Peach else PeachTint)
                .bounceClick(enabled = state == 0 && partner != null) {
                    val p = partner ?: return@bounceClick
                    val coupleId = me.coupleId ?: return@bounceClick
                    state = 1
                    scope.launch {
                        val ok = runCatching { app.signalRepository.send(coupleId, me, p.uid, SignalKind.Nudge) }.isSuccess
                        if (ok) SignalCenter.vibrate(context, SignalKind.Nudge)
                        state = if (ok) 2 else 0
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (state == 2) R.string.nudge_done else R.string.nudge_action),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
            )
        }
    }
}

private const val NUDGE_COOLDOWN_MS = 8_000L

/** "az önce", "5 dk önce"... */
@Composable
fun relativeTime(at: Long): String {
    val minutes = ((System.currentTimeMillis() - at) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.time_hours, minutes / 60)
        else -> stringResource(R.string.time_days, minutes / (24 * 60))
    }
}

/** Big friendly placeholder for tabs that arrive in later steps. */
@Composable
fun ComingSoon(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconCircle(icon, PeachTint, size = 96.dp, iconSize = 44.dp)
        Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brown, textAlign = TextAlign.Center)
        Text(body, fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft, textAlign = TextAlign.Center)
    }
}

@Composable
fun moodLabel(kind: com.lovelyy.app.data.MoodKind): String = stringResource(kind.labelRes())

fun formatDate(millis: Long): String =
    com.lovelyy.app.data.DayCounter.dateOf(millis).format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("tr")))
