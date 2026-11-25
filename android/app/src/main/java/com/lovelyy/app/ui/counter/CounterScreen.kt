package com.lovelyy.app.ui.counter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Milestone
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachCard
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")

fun formatDate(millis: Long): String = DayCounter.dateOf(millis).format(DateTimeFormatter.ofPattern("d MMMM yyyy", TR))
private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", TR))
private fun num(n: Long): String = NumberFormat.getIntegerInstance(TR).format(n)

/** Gün sayacı: how long you've been together, live down to the millisecond. */
@Composable
fun CounterScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val startMillis = couple?.startDate?.toDate()?.time
    var picking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
    ) {
        ScreenTitle(stringResource(R.string.feature_days), onBack) {
            if (startMillis != null) {
                CircleIconButton(Icons.Rounded.Edit, stringResource(R.string.counter_change), { picking = true })
            }
        }
        if (startMillis == null) {
            EmptyCounter(saving) { picking = true }
        } else {
            LiveCounter(startMillis)
            NextMilestone(startMillis)
            FunFacts(startMillis)
            MilestoneList(startMillis)
            TextButton(onClick = { picking = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
                Text(stringResource(R.string.counter_change), fontFamily = Quicksand, fontWeight = FontWeight.Bold, color = BrownSoft)
            }
        }
    }

    if (picking) {
        StartDatePicker(
            initial = startMillis,
            onDismiss = { picking = false },
            onPick = { date ->
                picking = false
                saving = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    runCatching { app.userRepository.setStartDate(couple?.id ?: return@launch, DayCounter.startMillis(date)) }
                    saving = false
                }
            },
        )
    }
}

@Composable
private fun EmptyCounter(saving: Boolean, onPick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(180.dp).softShadow(CircleShape, 14.dp).clip(CircleShape).background(Color(0xFFF8EDD7)),
        ) {
            Image(painterResource(R.drawable.cat_base), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(
            stringResource(R.string.counter_empty_title),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            stringResource(R.string.counter_empty_body),
            fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 32.dp),
        )
        PeachButton(stringResource(R.string.counter_pick), onPick, loading = saving)
    }
}

@Composable
private fun LiveCounter(startMillis: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { now = System.currentTimeMillis() } }
    val t = DayCounter.together(startMillis, now)
    SoftCard(Modifier.fillMaxWidth().padding(top = 18.dp), color = PeachCard, radius = 28.dp, padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.counter_together_long), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.counter_since, formatDate(startMillis)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft)
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Text(num(t.days), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 68.sp, color = Brown)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.counter_day_word), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BrownSoft, modifier = Modifier.padding(bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Favorite, null, tint = Peach, modifier = Modifier.padding(bottom = 14.dp).size(28.dp))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeBox("%02d".format(t.hours), stringResource(R.string.counter_hours), Modifier.weight(1f))
            TimeBox("%02d".format(t.minutes), stringResource(R.string.counter_minutes), Modifier.weight(1f))
            TimeBox("%02d".format(t.seconds), stringResource(R.string.counter_seconds), Modifier.weight(1f))
            TimeBox("%03d".format(t.millis), stringResource(R.string.counter_millis), Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimeBox(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(CreamCard).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brown, maxLines = 1)
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = BrownSoft, maxLines = 1)
    }
}

@Composable
private fun milestoneTitle(m: Milestone): String =
    if (m.years != null) stringResource(R.string.milestone_year, m.years) else stringResource(R.string.milestone_day, m.day)

@Composable
private fun NextMilestone(startMillis: Long) {
    val days = DayCounter.together(startMillis, System.currentTimeMillis()).days
    val (prev, next) = DayCounter.next(days)
    val from = prev?.day ?: 0L
    val target by animateFloatAsState(((days - from).toFloat() / (next.day - from).toFloat()).coerceIn(0f, 1f), label = "progress")
    val date = DayCounter.dateOf(startMillis).plusDays(next.day)
    SoftCard(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(PeachTint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Celebration, null, tint = Brown, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.counter_next, milestoneTitle(next)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                Text(formatDate(date), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
            }
            Text(stringResource(R.string.counter_left, next.day - days), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
        }
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(Cream)) {
            Box(Modifier.fillMaxWidth(target).height(8.dp).clip(CircleShape).background(Peach))
        }
    }
}

/** A few playful totals ("~41 milyon kalp atışı"). */
@Composable
private fun FunFacts(startMillis: Long) {
    val t = DayCounter.together(startMillis, System.currentTimeMillis())
    val hours = t.totalMillis / 3_600_000L
    val minutes = t.totalMillis / 60_000L
    SoftCard(Modifier.fillMaxWidth().padding(top = 14.dp), color = MintSoft) {
        Text(stringResource(R.string.counter_facts_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MintInk)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact(num(t.days / 7), stringResource(R.string.counter_weeks), Modifier.weight(1f))
            Fact(num(hours), stringResource(R.string.counter_hours), Modifier.weight(1f))
            Fact(num(minutes), stringResource(R.string.counter_minutes), Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.counter_heartbeats, num(minutes * 72)),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, color = MintInk,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun Fact(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(CreamCard).padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, maxLines = 1)
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = BrownSoft)
    }
}

@Composable
private fun MilestoneList(startMillis: Long) {
    val days = DayCounter.together(startMillis, System.currentTimeMillis()).days
    val all = DayCounter.milestones(days + 1500)
    val done = all.filter { it.day <= days }.takeLast(3)
    val upcoming = all.filter { it.day > days }.take(3)
    val start = DayCounter.dateOf(startMillis)
    Text(
        stringResource(R.string.counter_milestones),
        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
    SoftCard(Modifier.fillMaxWidth(), padding = 6.dp) {
        (done + upcoming).forEach { m ->
            val reached = m.day <= days
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (reached) MintSoft else Cream),
                    contentAlignment = Alignment.Center,
                ) {
                    if (reached) Icon(Icons.Rounded.Check, null, tint = MintInk, modifier = Modifier.size(18.dp))
                    else Text(if (m.isYear) "★" else "♥", color = Peach, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(milestoneTitle(m), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (reached) BrownSoft else Brown, modifier = Modifier.weight(1f))
                Text(formatDate(start.plusDays(m.day)), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePicker(initial: Long?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // The picker works in UTC days; convert our local date to that and back.
    val initialUtc = initial?.let { DayCounter.dateOf(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialUtc,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Cream,
        selectedDayContainerColor = Peach,
        selectedDayContentColor = Brown,
        todayDateBorderColor = Peach,
        todayContentColor = Brown,
        selectedYearContainerColor = Peach,
        selectedYearContentColor = Brown,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                },
            ) { Text(stringResource(R.string.action_save), fontFamily = Quicksand, fontWeight = FontWeight.Bold, color = Brown) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = Quicksand, color = BrownSoft) }
        },
        colors = colors,
    ) {
        DatePicker(
            state = state,
            colors = colors,
            title = {
                Text(
                    stringResource(R.string.counter_picker_title),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 12.dp),
                )
            },
        )
    }
}
