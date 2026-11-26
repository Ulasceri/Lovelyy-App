package com.lovelyy.app.ui.inbox

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.DeepLink
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.data.Signal
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.heartbeat.EmotionVisual
import com.lovelyy.app.ui.history.DayHeader
import com.lovelyy.app.ui.history.EmptyNote
import com.lovelyy.app.ui.history.InChip
import com.lovelyy.app.ui.history.InnerColumn
import com.lovelyy.app.ui.history.Muted
import com.lovelyy.app.ui.history.OutChip
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.history.TR
import com.lovelyy.app.ui.history.WHEEL_ICON_D
import com.lovelyy.app.ui.history.dayTitle
import com.lovelyy.app.ui.history.localDate
import com.lovelyy.app.ui.home.relativeTime
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.mood.MoodFace
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch

private val UnreadBg = Color(0xFFFFEFE4)
private val UnreadDot = Color(0xFFE0602B)
private val Butter = Color(0xFFFFF1C9)
private const val QUESTION_D = "M5 6h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-8l-4 3v-3H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z M10 10a2 2 0 1 1 2.5 1.9c-.3.1-.5.4-.5.7V13 M12 15.5h.01"
private const val NOTE_D = "M6 4h9l4 4v12H6z M15 4v4h4 M9 12h7M9 16h5"
private const val NUDGE_D = "M9 13V5.5a1.5 1.5 0 0 1 3 0V11l4.6 1.2a2 2 0 0 1 1.4 2.3l-.7 3.5a3 3 0 0 1-3 2.5H11a3 3 0 0 1-2.5-1.3L5.5 15a1.5 1.5 0 0 1 2.3-1.9z"
private const val CALENDAR_D = "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M4 10h16M8 3v4M16 3v4"
private const val READ_ALL_D = "M3 12.5l4 4L15 8.5M10 16.5l1 1L21 7.5"

/**
 * What the inbox has already shown, kept on this phone only: everything up to [floor] is read, plus single
 * signals tapped after that. Nothing about reading goes to Firestore (it is personal, not shared).
 */
object InboxSeen {
    private const val PREFS = "inbox"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class State(val floor: Long, val ids: Set<String>) {
        fun isRead(s: Signal) = s.time <= floor || s.id in ids
    }

    fun load(context: Context, coupleId: String): State {
        val p = prefs(context)
        // First open: only the last day counts as new, not the whole history.
        val floor = p.getLong("floor_$coupleId", System.currentTimeMillis() - 24 * 3_600_000L)
        return State(floor, p.getStringSet("ids_$coupleId", emptySet()).orEmpty())
    }

    fun markRead(context: Context, coupleId: String, id: String): State {
        val s = load(context, coupleId)
        val ids = (s.ids + id).toList().takeLast(200).toSet()
        prefs(context).edit().putLong("floor_$coupleId", s.floor).putStringSet("ids_$coupleId", ids).apply()
        return s.copy(ids = ids)
    }

    fun markAll(context: Context, coupleId: String, upTo: Long): State {
        prefs(context).edit().putLong("floor_$coupleId", upTo).putStringSet("ids_$coupleId", emptySet()).apply()
        return State(upTo, emptySet())
    }
}

/** Where tapping a signal takes you (same targets as the notifications). */
private fun routeOf(kind: SignalKind) = when (kind) {
    SignalKind.Heartbeat, SignalKind.Nudge -> DeepLink.HEARTBEAT
    SignalKind.Mood -> DeepLink.MOOD
    SignalKind.Question -> DeepLink.QUESTION
    SignalKind.Note -> DeepLink.NOTES
    SignalKind.Event -> DeepLink.PLANS
    SignalKind.Wheel -> DeepLink.WHEEL
}

/**
 * Bildirimler (approved board 26): everything the partner sent me, newest first, grouped by day. Unread ones are
 * tinted with a dot; a tap marks it read and opens its screen.
 */
@Composable
fun InboxScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val signals by remember(coupleId) { app.signalRepository.observeRecent(coupleId, 100).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(null)
    var seen by remember(coupleId) { mutableStateOf(InboxSeen.load(context, coupleId)) }
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val list = signals.orEmpty().filter { it.to == me.uid }
    val unread = list.count { !seen.isRead(it) }

    InnerColumn(spacing = 8) {
        CenteredTopBar(stringResource(R.string.ib_title), onBack) {
            RoundSvgButton(READ_ALL_D, stringResource(R.string.ib_read_all), {
                seen = InboxSeen.markAll(context, coupleId, maxOf(System.currentTimeMillis(), list.maxOfOrNull { it.time } ?: 0L))
                com.lovelyy.app.telemetry.Telemetry.track(com.lovelyy.app.analytics.AnalyticsEvent.InboxReadAll)
            })
        }
        if (signals != null && list.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            EmptyNote(stringResource(R.string.ib_empty, partnerName))
        }
        list.groupBy { it.time.localDate() }.entries.forEachIndexed { i, (day, items) ->
            Row(Modifier.fillMaxWidth().padding(top = if (i == 0) 4.dp else 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DayHeader(dayTitle(day), Modifier.weight(1f))
                if (i == 0 && unread > 0) {
                    Text(stringResource(R.string.ib_new, unread), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, modifier = Modifier.padding(end = 4.dp, top = 4.dp))
                }
            }
            items.forEach { s ->
                InboxRow(s, partnerName, read = seen.isRead(s)) {
                    seen = InboxSeen.markRead(context, coupleId, s.id)
                    onOpen(routeOf(s.kind))
                }
            }
        }
    }
}

@Composable
private fun InboxRow(s: Signal, partnerName: String, read: Boolean, onClick: () -> Unit) {
    val who = s.fromName.ifBlank { partnerName }
    val (title, sub) = texts(s, who)
    val unreadLabel = stringResource(R.string.ib_unread)
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(if (read) Color.White else UnreadBg)
            .bounceClick(pressedScale = 0.98f, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SignalIcon(s)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, color = Brown, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = SubText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(relativeTime(s.time), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted)
        }
        if (!read) Box(Modifier.size(10.dp).clip(CircleShape).background(UnreadDot).semantics { contentDescription = unreadLabel })
    }
}

@Composable
private fun texts(s: Signal, who: String): Pair<String, String?> = when (s.kind) {
    SignalKind.Heartbeat -> stringResource(R.string.hi_hb_in, who, stringResource((s.emotion ?: Emotion.Sevgi).labelRes()).lowercase(TR)) to null
    SignalKind.Nudge -> stringResource(R.string.hi_hb_nudge_in, who) to null
    SignalKind.Mood -> stringResource(R.string.ib_mood, who, stringResource((s.mood ?: MoodKind.Mutlu).labelRes())) to null
    SignalKind.Question -> stringResource(R.string.ib_question, who) to stringResource(R.string.ib_question_sub)
    SignalKind.Note -> stringResource(R.string.ib_note, who) to null
    SignalKind.Event -> stringResource(R.string.ib_event, who) to s.text?.takeIf { it.isNotBlank() }
    SignalKind.Wheel -> {
        // Sent as "wheel label → result".
        val parts = s.text.orEmpty().split(" → ", limit = 2)
        val result = parts.getOrNull(1) ?: parts[0]
        stringResource(R.string.ib_wheel, who, result) to parts.takeIf { it.size == 2 }?.let { stringResource(R.string.ib_wheel_sub, it[0]) }
    }
}

@Composable
private fun SignalIcon(s: Signal) {
    when (s.kind) {
        SignalKind.Heartbeat -> Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            EmotionVisual(s.emotion ?: Emotion.Sevgi, Modifier.size(40.dp), heartScale = 0.5f)
        }
        SignalKind.Mood -> MoodFace(s.mood ?: MoodKind.Mutlu, Modifier.size(44.dp))
        else -> {
            val (d, bg) = when (s.kind) {
                SignalKind.Question -> QUESTION_D to OutChip
                SignalKind.Note -> NOTE_D to Butter
                SignalKind.Nudge -> NUDGE_D to InChip
                SignalKind.Event -> CALENDAR_D to InChip
                else -> WHEEL_ICON_D to OutChip
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                SvgStrokeIcon(d, Brown, Modifier.size(22.dp))
            }
        }
    }
}
