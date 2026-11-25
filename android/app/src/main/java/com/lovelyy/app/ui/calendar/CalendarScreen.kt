package com.lovelyy.app.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.CalEvent
import com.lovelyy.app.data.CalendarMath
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.EventRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.DesignField
import com.lovelyy.app.ui.components.LovelyyDatePicker
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachCard
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private fun fmt(d: LocalDate, pattern: String = "d MMMM yyyy") = d.format(DateTimeFormatter.ofPattern(pattern, TR))

val EventEmojis = listOf("💛", "🎂", "💍", "✈️", "🍽️", "🎬", "🎉", "🏖️", "🎁", "📌")

/** Takvim + özel günler. [onBack] is null when shown as the Planlar tab. */
@Composable
fun CalendarScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: (() -> Unit)?) {
    val app = lovelyyApp()
    val scope = rememberCoroutineScope()
    val coupleId = me.coupleId ?: return
    val stored by remember(coupleId) { app.eventRepository.observe(coupleId).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(emptyList())
    val anniversaryTitle = stringResource(R.string.cal_anniversary)
    val startMillis = couple?.startDate?.toDate()?.time
    // The day counter's start date is our anniversary: a built-in special day.
    val events = remember(stored, startMillis, anniversaryTitle) {
        stored + listOfNotNull(
            startMillis?.let { CalEvent("anniversary", anniversaryTitle, DayCounter.dateOf(it), "💞", yearly = true, by = "", byName = "", builtIn = true) },
        )
    }
    val today = remember { LocalDate.now() }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var editing by remember { mutableStateOf<CalEvent?>(null) }
    var creating by remember { mutableStateOf(false) }
    val ym = YearMonth.parse(month)
    val sel = LocalDate.parse(selected)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                .padding(top = if (onBack == null) 16.dp else 0.dp, bottom = if (onBack == null) 190.dp else 110.dp),
        ) {
            if (onBack != null) ScreenTitle(stringResource(R.string.feature_calendar), onBack)
            else Text(stringResource(R.string.tab_plans), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown, modifier = Modifier.padding(top = 8.dp))

            // Special days with a countdown.
            val special = events.filter { it.yearly }
                .mapNotNull { e -> CalendarMath.daysUntil(e, today)?.let { e to it } }
                .sortedBy { it.second }
            Text(
                stringResource(R.string.cal_special), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
            )
            if (special.isEmpty()) {
                SoftCard(Modifier.fillMaxWidth(), color = PeachCard) {
                    Text(stringResource(R.string.cal_special_empty), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Brown)
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    special.forEach { (e, days) -> SpecialCard(e, days, today) { if (!e.builtIn) editing = e } }
                }
            }

            // Month grid.
            SoftCard(Modifier.fillMaxWidth().padding(top = 18.dp), radius = 26.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.cal_prev), { month = ym.minusMonths(1).toString() }, size = 40.dp, background = Cream)
                    Text(
                        ym.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", TR)).replaceFirstChar { it.titlecase(TR) },
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cal_next), { month = ym.plusMonths(1).toString() }, size = 40.dp, background = Cream)
                }
                MonthGrid(ym, today, sel, events) { d ->
                    selected = d.toString()
                    if (YearMonth.from(d) != ym) month = YearMonth.from(d).toString()
                }
            }

            // The chosen day.
            val dayEvents = events.filter { CalendarMath.occursOn(it, sel) }
            Text(
                fmt(sel, "d MMMM, EEEE"), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
            )
            if (dayEvents.isEmpty()) {
                Text(stringResource(R.string.cal_day_empty), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dayEvents.forEach { e -> EventRow(e, me.uid, partner?.displayName.orEmpty(), sel) { if (!e.builtIn) editing = e } }
                }
            }

            // Upcoming plans (one-time).
            val upcoming = events.filter { !it.yearly && !it.date.isBefore(today) }.sortedBy { it.date }.take(5)
            if (upcoming.isNotEmpty()) {
                Text(
                    stringResource(R.string.cal_upcoming), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                    modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    upcoming.forEach { e -> EventRow(e, me.uid, partner?.displayName.orEmpty(), null) { editing = e } }
                }
            }
        }
        PeachButton(
            stringResource(R.string.cal_add), { creating = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = if (onBack == null) 112.dp else 20.dp),
            leading = { Icon(Icons.Rounded.Add, null, tint = Brown, modifier = Modifier.size(22.dp)) },
        )
    }

    if (creating || editing != null) {
        val e = editing
        EventEditor(
            initial = e,
            defaultDate = sel,
            onDismiss = { creating = false; editing = null },
            onSave = { title, date, emoji, yearly ->
                creating = false; editing = null
                scope.launch {
                    runCatching {
                        app.eventRepository.save(coupleId, me, e?.id, title, date, emoji, yearly)
                        if (e == null && partner != null) app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Event, text = title)
                    }
                }
                selected = date.toString()
                month = YearMonth.from(date).toString()
            },
            onDelete = e?.let { ev -> { scope.launch { runCatching { app.eventRepository.delete(coupleId, ev.id) } }; editing = null } },
        )
    }
}

@Composable
private fun SpecialCard(e: CalEvent, days: Long, today: LocalDate, onClick: () -> Unit) {
    val next = CalendarMath.nextOccurrence(e, today) ?: return
    val isToday = days == 0L
    val years = CalendarMath.yearsAt(e, next)
    SoftCard(Modifier.width(150.dp), color = if (isToday) Peach else CreamCard, radius = 22.dp, padding = 14.dp, shadow = true, onClick = onClick) {
        Text(e.emoji, fontSize = 28.sp)
        Text(e.title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        if (years > 0) Text(stringResource(R.string.cal_years, years), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
        Text(
            if (isToday) stringResource(R.string.cal_today) else stringResource(R.string.counter_left, days),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (isToday) Brown else PeachDeep,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(fmt(next, "d MMMM"), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
    }
}

@Composable
private fun MonthGrid(ym: YearMonth, today: LocalDate, selected: LocalDate, events: List<CalEvent>, onPick: (LocalDate) -> Unit) {
    Row(Modifier.padding(top = 12.dp)) {
        DayOfWeek.entries.forEach { d ->
            Text(
                d.getDisplayName(TextStyle.SHORT, TR).take(3), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
    val first = ym.atDay(1)
    val lead = first.dayOfWeek.value - 1 // Monday first
    val cells = (0 until 42).map { first.minusDays(lead.toLong()).plusDays(it.toLong()) }
    val rows = cells.chunked(7).filter { week -> week.any { YearMonth.from(it) == ym } }
    rows.forEach { week ->
        Row(Modifier.padding(top = 4.dp)) {
            week.forEach { d ->
                val inMonth = YearMonth.from(d) == ym
                val isSel = d == selected
                val has = events.filter { CalendarMath.occursOn(it, d) }
                val bg by animateColorAsState(if (isSel) Peach else Color.Transparent, label = "day")
                Column(
                    Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(bg)
                        .then(if (d == today && !isSel) Modifier.border(1.5.dp, Peach, CircleShape) else Modifier)
                        .bounceClick(pressedScale = 0.9f) { onPick(d) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        d.dayOfMonth.toString(), fontFamily = Quicksand,
                        fontWeight = if (isSel || d == today) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp,
                        color = if (inMonth) Brown else Color(0xFFC9BBB2),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
                        has.take(3).forEach { e ->
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) Brown else if (e.yearly) PeachDeep else Mint))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: CalEvent, myUid: String, partnerName: String, on: LocalDate?, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = if (e.builtIn) null else onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (e.yearly) PeachTint else MintSoft), contentAlignment = Alignment.Center) {
                Text(e.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildList {
                    if (on == null) add(fmt(e.date, "d MMMM, EEEE"))
                    if (e.yearly) add(stringResource(R.string.cal_yearly_badge))
                    if (!e.builtIn) add(if (e.by == myUid) stringResource(R.string.cal_added_by_you) else stringResource(R.string.cal_added_by, e.byName.ifBlank { partnerName }))
                }
                Text(sub.joinToString(" · "), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (e.yearly) PeachDeep else MintInk)
            }
            if (!e.builtIn) Icon(Icons.Rounded.Edit, null, tint = BrownSoft, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EventEditor(
    initial: CalEvent?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: EventEmojis.first()) }
    var yearly by remember { mutableStateOf(initial?.yearly ?: false) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(20.dp).imePadding().fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Cream).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (initial == null) R.string.cal_new else R.string.cal_edit),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.weight(1f),
                )
                if (onDelete != null) CircleIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.notes_delete), { if (confirmDelete) onDelete() else confirmDelete = true })
            }
            if (confirmDelete) {
                Text(stringResource(R.string.notes_delete_confirm), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PeachDeep, modifier = Modifier.padding(top = 8.dp))
            }
            DesignField(
                title, { title = it.take(EventRepository.MAX_TITLE) }, stringResource(R.string.cal_title_hint),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.padding(top = 14.dp),
            )
            // Date.
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(CreamCard)
                    .bounceClick { picking = true }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = BrownSoft, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(fmt(date, "d MMMM yyyy, EEEE"), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Brown)
            }
            // Emoji.
            Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventEmojis.forEach { em ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (em == emoji) PeachTint else CreamCard)
                            .border(if (em == emoji) 2.dp else 0.dp, if (em == emoji) Peach else Color.Transparent, CircleShape)
                            .bounceClick { emoji = em },
                        contentAlignment = Alignment.Center,
                    ) { Text(em, fontSize = 20.sp) }
                }
            }
            // Special day toggle.
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CreamCard).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cal_yearly), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                    Text(stringResource(R.string.cal_yearly_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
                }
                Switch(
                    checked = yearly, onCheckedChange = { yearly = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Peach, checkedThumbColor = CreamCard, checkedBorderColor = Peach),
                )
            }
            PeachButton(stringResource(R.string.action_save), { onSave(title, date, emoji, yearly) }, enabled = title.isNotBlank(), modifier = Modifier.padding(top = 18.dp))
            Text(
                stringResource(R.string.action_cancel), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrownSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).bounceClick(onClick = onDismiss).padding(12.dp),
            )
        }
    }
    if (picking) {
        LovelyyDatePicker(
            initial = date, title = stringResource(R.string.cal_pick_date), allowFuture = true,
            onDismiss = { picking = false }, onPick = { date = it; picking = false },
        )
    }
}
