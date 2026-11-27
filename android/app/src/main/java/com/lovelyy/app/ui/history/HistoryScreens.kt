package com.lovelyy.app.ui.history

import com.lovelyy.app.text.AppLocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodDay
import com.lovelyy.app.data.QuestionMath
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.heartbeat.EmotionVisual
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.mood.MoodFace
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import java.time.DayOfWeek
import java.time.LocalDate

private const val NUDGE_D = "M9 13V5.5a1.5 1.5 0 0 1 3 0V11l4.6 1.2a2 2 0 0 1 1.4 2.3l-.7 3.5a3 3 0 0 1-3 2.5H11a3 3 0 0 1-2.5-1.3L5.5 15a1.5 1.5 0 0 1 2.3-1.9z M15 4.5a5 5 0 0 1 1.5 3.5M5.5 8a5 5 0 0 1 1.5-3.5"
internal const val WHEEL_ICON_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z M12 3.5v7M12 13.5v7M3.5 12h7M13.5 12h7M6 6l4.9 4.9M13.1 13.1L18 18M18 6l-4.9 4.9M10.9 13.1L6 18"
private const val STAR_D = "M12 4l2.5 5 5.5.8-4 3.9.9 5.5-4.9-2.6-4.9 2.6.9-5.5-4-3.9 5.5-.8z"
private val WheelTints = listOf(Color(0xFFFFE6D6), Color(0xFFD6F0E8), Color(0xFFFFF1C9))

// ---------- Ruh hali geçmişi (board 20) ----------

@Composable
fun MoodHistoryScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val log by remember(coupleId) { app.moodRepository.observeHistory(coupleId).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(null)
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = LocalDate.now()
    val first = today.minusDays(29)
    val byDay = log.orEmpty().groupBy { it.day }

    InnerColumn(spacing = 12) {
        CenteredTopBar(stringResource(R.string.hi_mood_title), onBack)
        WhiteCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.hi_last_30), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, modifier = Modifier.weight(1f))
                Text("${first.format(DAY_MONTH)} – ${today.format(DAY_MONTH)}", fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted)
            }
            Spacer(Modifier.height(10.dp))
            // Monday-first calendar of the last 30 days: two little faces per day (left = me, right = partner).
            val weekdays = listOf(R.string.hi_mon, R.string.hi_tue, R.string.hi_wed, R.string.hi_thu, R.string.hi_fri, R.string.hi_sat, R.string.hi_sun)
            Row { weekdays.forEach { Text(stringResource(it), Modifier.weight(1f), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center) } }
            val lead = first.dayOfWeek.value - DayOfWeek.MONDAY.value
            val cells: List<LocalDate?> = List(lead) { null } + (0L..29L).map { first.plusDays(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.padding(top = 4.dp)) {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(0.82f).padding(1.dp), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val entries = byDay[day].orEmpty()
                                val mine = entries.firstOrNull { it.entry.uid == me.uid }?.entry?.kind
                                val theirs = entries.firstOrNull { it.entry.uid != me.uid }?.entry?.kind
                                Column(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(if (day == today) PeachSoft else Color.Transparent),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        gridDayLabel(day, first) { it.format(DAY_MONTH_SHORT) },
                                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Muted, maxLines = 1, softWrap = false,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        SmallFace(mine)
                                        SmallFace(theirs)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniAvatar(me.displayName, Peach)
                Text(stringResource(R.string.hi_left_face), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubText)
                Text("·", color = Muted)
                MiniAvatar(partnerName, Mint)
                Text(stringResource(R.string.hi_right_face, partnerName), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubText)
            }
        }

        if (log != null && log.orEmpty().isEmpty()) EmptyNote(stringResource(R.string.hi_mood_empty))
        byDay.keys.sortedDescending().forEach { day ->
            DayHeader(if (day == today) stringResource(R.string.hi_today_long, day.format(DAY_MONTH)) else dayTitle(day))
            WhiteCard(padding = 14.dp) {
                byDay.getValue(day).sortedBy { it.entry.at }.forEachIndexed { i, d ->
                    if (i > 0) Spacer(Modifier.height(12.dp))
                    MoodLine(d, d.entry.uid == me.uid, if (d.entry.uid == me.uid) me.displayName else partnerName)
                }
            }
        }
    }
}

@Composable
private fun SmallFace(kind: com.lovelyy.app.data.MoodKind?) {
    if (kind != null) MoodFace(kind, Modifier.size(15.dp))
    else Box(Modifier.size(15.dp).clip(CircleShape).border(1.dp, Color(0xFFE3D5CB), CircleShape))
}

@Composable
private fun MoodLine(d: MoodDay, mine: Boolean, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MoodFace(d.entry.kind, Modifier.size(36.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (mine) stringResource(R.string.cat_you) else name) + " · " + d.entry.at.clock(),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted,
            )
            Text(stringResource(d.entry.kind.labelRes()), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
        }
        MiniAvatar(name, if (mine) Peach else Mint, 26)
    }
    if (d.entry.note.isNotBlank()) {
        Text(
            d.entry.note, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Brown,
            modifier = Modifier.padding(top = 6.dp, start = 46.dp).clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp))
                .background(if (mine) PeachBubble else MintBubble).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ---------- Günün sorusu geçmişi (board 21) ----------

@Composable
fun QuestionHistoryScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onAnswer: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val days by remember(coupleId) { app.questionRepository.observeRecent(coupleId, 365).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(null)
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = LocalDate.now()
    val list = days.orEmpty().sortedByDescending { it.day }
    val todayQ = list.firstOrNull { it.day == today.toString() }
    // Today is listed once I answered it (until then it's the "Cevapla" card above).
    val past = list.filter { it.answers.isNotEmpty() && (it.day != today.toString() || it.answers.containsKey(me.uid)) }
    val both = list.count { it.answers.size >= 2 }
    val streak = QuestionMath.streak(list, today)

    InnerColumn(spacing = 12) {
        CenteredTopBar(stringResource(R.string.hi_q_title), onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(R.plurals.hi_q_count, both.toInt(), both), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                modifier = Modifier.clip(CircleShape).background(PeachSoft).padding(horizontal = 12.dp, vertical = 7.dp),
            )
            Spacer(Modifier.weight(1f))
            SvgStrokeIcon(STAR_D, Color(0xFFB5532A), Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(pluralStringResource(R.plurals.q_streak, streak.toInt(), streak), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
        }
        // Today's question, if I haven't answered yet.
        if (todayQ?.answers?.containsKey(me.uid) != true) {
            val q = todayQ?.question ?: com.lovelyy.app.data.QuestionBank.forDay(today)
            val partnerDone = todayQ?.answers?.keys?.any { it != me.uid } == true
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(PeachSoft).padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.hi_today) + if (partnerDone) " · " + stringResource(R.string.hi_q_partner_done, partnerName) else "",
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SubText,
                    )
                    Text(q, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, color = Brown)
                }
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Peach).bounceClick(onClick = onAnswer).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.hi_q_answer), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown) }
            }
        }
        if (days != null && past.isEmpty()) EmptyNote(stringResource(R.string.hi_q_empty))
        past.forEach { d ->
            val date = runCatching { LocalDate.parse(d.day) }.getOrNull()
            val iAnswered = d.answers.containsKey(me.uid)
            WhiteCard(padding = 16.dp) {
                Text(date?.format(DAY_MONTH) ?: d.day, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Muted)
                Text(d.question, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, color = Brown, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                val mine = d.answers[me.uid]
                val theirs = d.answers.values.firstOrNull { it.uid != me.uid }
                AnswerBubble(me.displayName, Peach, PeachBubble, mine?.text ?: stringResource(R.string.hi_q_no_answer))
                // The partner's answer stays hidden if I never answered that day (same rule as the question screen).
                AnswerBubble(
                    partnerName, Mint, MintBubble,
                    when {
                        theirs == null -> stringResource(R.string.hi_q_no_answer)
                        !iAnswered -> stringResource(R.string.hi_q_hidden)
                        else -> theirs.text
                    },
                )
            }
        }
        if (paged.canLoadMore) {
            com.lovelyy.app.ui.components.LoadMoreButton(paged.loadMore, Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        }
    }
}

/** Signals per history page (heartbeats are mixed with the other kinds, so the page is a bit larger). */
private const val HISTORY_PAGE = 50
private const val WHEEL_PAGE = 30

@Composable
private fun AnswerBubble(name: String, avatar: Color, bubble: Color, text: String) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.padding(top = 4.dp)) { MiniAvatar(name, avatar, 26) }
        Text(
            text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Brown,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)).background(bubble).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ---------- Kalp atışı geçmişi (board 22) ----------

@Composable
fun HeartbeatHistoryScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    // One page at a time (each signal is a billed read); all kinds come back, so a page shows fewer than 50 rows.
    val paged = com.lovelyy.app.ui.components.rememberPaged(coupleId, HISTORY_PAGE) { app.signalRepository.observeRecent(coupleId, it.toLong()) }
    val signals = paged.items
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val list = signals.orEmpty()
        .filter { it.kind == SignalKind.Heartbeat || it.kind == SignalKind.Nudge }
        .filter { when (filter) { 1 -> it.from == me.uid; 2 -> it.from != me.uid; else -> true } }

    InnerColumn {
        CenteredTopBar(stringResource(R.string.hi_hb_title), onBack)
        FilterChips(
            listOf(stringResource(R.string.hi_all), stringResource(R.string.hi_sent), stringResource(R.string.hi_received)),
            filter, { filter = it }, Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        if (signals != null && list.isEmpty()) EmptyNote(stringResource(R.string.hi_hb_empty))
        list.groupBy { (it.createdAt ?: it.at).localDate() }.forEach { (day, items) ->
            DayHeader(dayTitle(day))
            items.forEach { s ->
                val mine = s.from == me.uid
                val nudge = s.kind == SignalKind.Nudge
                val emotion = s.emotion ?: Emotion.Sevgi
                val label = if (nudge) "" else stringResource(emotion.labelRes()).lowercase(AppLocale.current())
                val title = when {
                    nudge && mine -> stringResource(R.string.hi_hb_nudge_out, com.lovelyy.app.text.TurkishSuffix.accusative(partnerName))
                    nudge -> stringResource(R.string.hi_hb_nudge_in, partnerName)
                    mine -> stringResource(R.string.hi_hb_out, com.lovelyy.app.text.TurkishSuffix.dative(partnerName), label)
                    else -> stringResource(R.string.hi_hb_in, partnerName, label)
                }
                HistoryRow(
                    icon = {
                        if (nudge) SvgStrokeIcon(NUDGE_D, Brown, Modifier.size(24.dp))
                        else EmotionVisual(emotion, Modifier.size(40.dp), heartScale = 0.5f)
                    },
                    iconBg = if (nudge) InChip else Color.Transparent,
                    title = title,
                    subtitle = stringResource(if (nudge) R.string.hi_hb_kind_nudge else R.string.hi_hb_kind_beat) + " · " + (s.createdAt ?: s.at).clock(),
                    trailing = { DirectionChip(incoming = !mine) },
                )
            }
        }
        if (paged.canLoadMore) {
            com.lovelyy.app.ui.components.LoadMoreButton(paged.loadMore, Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        }
    }
}

// ---------- Karar çarkı geçmişi (board 23) ----------

@Composable
fun WheelHistoryScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val paged = com.lovelyy.app.ui.components.rememberPaged(coupleId, WHEEL_PAGE) { app.wheelRepository.observeLog(coupleId, it.toLong()) }
    val log = paged.items
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val wheels = log.orEmpty().map { it.wheelId to it.label }.distinctBy { it.first }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val list = log.orEmpty().filter { filter == null || it.wheelId == filter }

    InnerColumn {
        CenteredTopBar(stringResource(R.string.hi_wheel_title), onBack)
        FilterChips(
            listOf(stringResource(R.string.hi_all)) + wheels.map { it.second },
            wheels.indexOfFirst { it.first == filter } + 1,
            { i -> filter = if (i == 0) null else wheels[i - 1].first },
            Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        if (log != null && list.isEmpty()) EmptyNote(stringResource(R.string.hi_wheel_empty))
        list.groupBy { it.at.localDate() }.forEach { (day, items) ->
            DayHeader(dayTitle(day))
            items.forEach { s ->
                val mine = s.by == me.uid
                val tint = WheelTints[(wheels.indexOfFirst { it.first == s.wheelId }.coerceAtLeast(0)) % WheelTints.size]
                HistoryRow(
                    icon = { SvgStrokeIcon(WHEEL_ICON_D, Brown, Modifier.size(24.dp)) },
                    iconBg = tint,
                    title = s.result,
                    subtitle = null,
                    trailing = { Text(s.at.clock(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted) },
                    extra = {
                        Text(s.label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Muted)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MiniAvatar(if (mine) me.displayName else s.byName.ifBlank { partnerName }, if (mine) Peach else Mint)
                            Text(
                                if (mine) stringResource(R.string.hi_wheel_by_me) else stringResource(R.string.hi_wheel_by, s.byName.ifBlank { partnerName }),
                                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubText,
                            )
                        }
                    },
                )
            }
        }
    }
}
