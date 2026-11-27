package com.lovelyy.app.ui.cat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.CareStreak
import com.lovelyy.app.data.CoupleClock
import com.lovelyy.app.data.CoupleDay
import com.lovelyy.app.data.PlusPrefs
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.data.StreakProtection
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.text.AppLocale
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.analytics.InnerScreens
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.plus.LocalPlus
import com.lovelyy.app.ui.plus.PaywallSource
import com.lovelyy.app.ui.plus.PlusPill
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownMuted
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.SheetHandle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** The design's calendar icon (24×24). */
private const val CALENDAR_D = "M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z M3 10h18 M8 3v4 M16 3v4"
/** The design's "refresh" icon (made-up day hint). */
private const val REFRESH_D = "M4 12a8 8 0 1 0 2.3-5.7 M4 4v4h4"

/**
 * The couple's "today" for the care streak: the Plus time from Home ([LocalPlus], the P8 debug clock in debug
 * builds, real time in release) in the couple's zone, so streak protection can be tried on an emulator with
 * "+1 gün". Outside Home it is real time.
 */
@Composable
internal fun streakToday(): LocalDate {
    val zone by CoupleClock.zone.collectAsStateWithLifecycle()
    val now = LocalPlus.current?.now ?: System.currentTimeMillis()
    return CoupleDay.today(zone, now)
}

/** "2 sa 40 dk" / "40 dk" until the couple's midnight. */
@Composable
internal fun timeLeftText(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return if (hours > 0) stringResource(R.string.streak_time_hm, hours, minutes) else stringResource(R.string.streak_time_m, minutes)
}

/**
 * P5a "Seri tehlikede" (board PlusSeriTehlike), with the P5b "İzin günü" sheet it opens. Shown on Yuva (under the
 * care buttons, in the Plus card slot) and on Kedi bakımı, after 20:00 in the couple's zone while today's care is
 * missing (StreakProtection.showDangerCard), never on the same day as the day-12 trial card. The free way first (nudge the partner), the day off beside it; the
 * care buttons always stay open. Without Plus, "İzin günü kullan" opens the plans. Nothing outside Home.
 */
@Composable
fun StreakDangerSection(
    vm: CatViewModel,
    me: UserProfile,
    partner: UserProfile?,
    catName: String,
    modifier: Modifier = Modifier,
) {
    val plus = LocalPlus.current ?: return
    val coupleId = me.coupleId ?: return
    val ui by vm.ui.collectAsStateWithLifecycle()
    val streak = ui?.doc?.streak ?: return
    val zone by CoupleClock.zone.collectAsStateWithLifecycle()
    // Plus time (real time in release; the P8 debug clock in debug builds), refreshed every minute by Home.
    val now = plus.now
    val today = CoupleDay.today(zone, now)
    val hour = Instant.ofEpochMilli(now).atZone(zone).hour
    val context = LocalContext.current
    // The day-12 trial card (P2b) is the day's one Plus surface that appears by itself: no danger card beside it.
    val trialCardShown = PlusRules.showDay12Card(plus.info, now, PlusPrefs.cardDismissedOn(context, coupleId, "day12") == today.toString())
    if (!StreakProtection.showDangerCard(streak, today, hour, trialCardShown)) return

    LaunchedEffect(coupleId, today) {
        if (PlusPrefs.dangerShownOn(context, coupleId) != today.toString()) {
            PlusPrefs.markDangerShown(context, coupleId, today.toString())
            vm.dangerCardShown(plus.isPlus)
        }
    }
    var dayOffOpen by rememberSaveable { mutableStateOf(false) }
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val meDone = streak.caredToday(me.uid, today)
    val partnerDone = partner?.uid?.let { streak.caredToday(it, today) } == true

    StreakDangerCard(
        count = streak.current(today),
        catName = catName,
        partnerName = partnerName,
        meDone = meDone,
        partnerDone = partnerDone,
        millisLeft = StreakProtection.millisToMidnight(now, zone),
        // Nudging only helps while the partner still has to care.
        onNudge = partner?.takeIf { !partnerDone }?.let { p -> { vm.nudge(me, p.uid) } },
        onDayOff = {
            if (plus.isPlus) dayOffOpen = true else plus.openPlans(PaywallSource.STREAK_DAY_OFF)
        },
        modifier = modifier,
    )
    LaunchedEffect(dayOffOpen) { if (dayOffOpen) Telemetry.onScreen(InnerScreens.STREAK_DAY_OFF) }
    if (dayOffOpen) {
        DayOffSheet(
            streak = streak,
            today = today,
            catName = catName,
            onUse = { vm.takeDayOff(plus.isPlus) },
            onDismiss = { dayOffOpen = false },
        )
    }
}

@Composable
private fun StreakDangerCard(
    count: Int,
    catName: String,
    partnerName: String,
    meDone: Boolean,
    partnerDone: Boolean,
    millisLeft: Long,
    onNudge: (() -> Unit)?,
    onDayOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catTo = TurkishSuffix.dative(catName)
    val left = timeLeftText(millisLeft)
    val body = when {
        meDone && !partnerDone -> pluralStringResource(R.plurals.streak_danger_body_partner_waiting, count, count, catTo, partnerName, left)
        !meDone && partnerDone -> pluralStringResource(R.plurals.streak_danger_body_me_waiting, count, count, catTo, partnerName, left)
        else -> pluralStringResource(R.plurals.streak_danger_body_both_waiting, count, count, catTo, partnerName, left)
    }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PeachSoft).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(CreamCard), contentAlignment = Alignment.Center) { StreakFlame(24.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    stringResource(R.string.streak_danger_title),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = Brown,
                    modifier = Modifier.semantics { heading() },
                )
                Text(body, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onNudge != null) {
                SmallAction(stringResource(R.string.streak_danger_nudge, TurkishSuffix.accusative(partnerName)), Peach, null, onNudge, Modifier.weight(1f))
            }
            SmallAction(stringResource(R.string.streak_danger_day_off), CreamCard, CALENDAR_D, onDayOff, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SmallAction(text: String, fill: androidx.compose.ui.graphics.Color, icon: String?, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(fill)
            .semantics { role = Role.Button }
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) SvgStrokeIcon(icon, Brown, Modifier.size(16.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreakSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(hideThen: (() -> Unit) -> Unit) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action -> scope.launch { sheet.hide() }.invokeOnCompletion { action() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Cream,
        scrimColor = Brown.copy(alpha = 0.45f),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(SheetHandle))
        },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content(hideThen) }
    }
}

/**
 * P5b "Bugünü izin günü say" (board PlusSeriDondur), Plus only: today counts as a day off, the streak keeps its
 * count and continues tomorrow. Two a month for the couple, renewed on the 1st; when none is left the button is
 * off and the renewal day is written.
 */
@Composable
private fun DayOffSheet(streak: CareStreak, today: LocalDate, catName: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    val count = streak.current(today)
    val left = StreakProtection.freezesLeft(streak, today)
    val used = StreakProtection.freezeDaysThisMonth(streak, today)
    val month = today.format(AppLocale.formatter("LLLL")).replaceFirstChar { it.titlecase(AppLocale.current()) }
    fun dayText(d: LocalDate) = TurkishSuffix.locative(d.format(AppLocale.formatter("dMMMM")))
    val title = stringResource(R.string.streak_off_title)
    StreakSheet(onDismiss) { hideThen ->
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)) { PlusPill(stringResource(R.string.streak_off_pill), fontSize = 12) }
        Text(
            title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = Brown,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
        )
        Text(
            pluralStringResource(R.plurals.streak_off_body, count, count, TurkishSuffix.dative(catName), count + 1),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = BrownSoft,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val cardShape = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().softShadow(cardShape, 6.dp).clip(cardShape).background(CreamCard).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(StreakProtection.FREEZES_PER_MONTH) { i -> DayOffPip(available = i < left) }
            }
            Column(Modifier.weight(1f)) {
                val total = StreakProtection.FREEZES_PER_MONTH
                Text(
                    when (left) {
                        0 -> stringResource(R.string.streak_off_left_none, month)
                        total -> stringResource(R.string.streak_off_left_all, month, total)
                        else -> stringResource(R.string.streak_off_left_one, month, total)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
                )
                Text(
                    when {
                        used.size >= 2 -> stringResource(R.string.streak_off_used_two, dayText(used[0]), dayText(used[1]), dayText(StreakProtection.freezesRenewOn(today)))
                        used.size == 1 -> stringResource(R.string.streak_off_used_one, dayText(used[0]))
                        else -> stringResource(R.string.streak_off_used_none)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = BrownSoft,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MintTint).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SvgStrokeIcon(REFRESH_D, MintInk, Modifier.size(20.dp))
            Text(
                stringResource(R.string.streak_off_repair_hint),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = MintInk, modifier = Modifier.weight(1f),
            )
        }
        PeachButton(
            stringResource(R.string.streak_off_use),
            onClick = { hideThen { onUse(); onDismiss() } },
            modifier = Modifier.padding(top = 4.dp),
            enabled = left > 0,
            leading = { SvgStrokeIcon(CALENDAR_D, Brown, Modifier.size(20.dp)) },
        )
        GhostButton(stringResource(R.string.streak_off_cancel), { hideThen(onDismiss) }, Modifier.align(Alignment.CenterHorizontally))
    }
}

/** A day off of this month: mint with a calendar while it is there, a ring with a check once used. */
@Composable
private fun DayOffPip(available: Boolean) {
    val label = stringResource(if (available) R.string.streak_off_available else R.string.streak_off_used)
    if (available) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Mint).semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { SvgStrokeIcon(CALENDAR_D, Brown, Modifier.size(20.dp)) }
    } else {
        Box(
            Modifier.size(40.dp).clip(CircleShape).border(2.dp, Kit.Dashed, CircleShape).semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { SvgStrokeIcon(Kit.CHECK_D, BrownSoft, Modifier.size(16.dp), strokeWidth = 2.4f) }
    }
}

/**
 * P5c "Seriniz bitti" (board PlusSeriOnar): opens by itself on the first open after the streak broke, for the rest
 * of that day (24 hours after the break at midnight). With Plus the button makes up yesterday ("Dünü telafi et")
 * and there is no Plus strip; without Plus it shows the strip and "Plus ile telafi et" opens the plans.
 * "Seriye 1. günden başla" is always free.
 */
@Composable
fun StreakRepairSheet(
    broken: StreakProtection.Broken,
    catName: String,
    millisLeft: Long,
    hasPlus: Boolean,
    onMakeUp: () -> Unit,
    onPlus: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The streak that ended (before a new day 1 was added today).
    val old = if (broken.restartedToday) broken.count - 1 else broken.count
    val title = pluralStringResource(R.plurals.streak_repair_title, old, old)
    StreakSheet(onDismiss) { hideThen ->
        Text(
            title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = Brown,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp).semantics { heading() },
        )
        Text(
            pluralStringResource(R.plurals.streak_repair_body, old, old, TurkishSuffix.dative(catName)),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = BrownSoft,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val cardShape = RoundedCornerShape(24.dp)
        Row(
            Modifier.fillMaxWidth().softShadow(cardShape, 6.dp).clip(cardShape).background(CreamCard).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(PeachTint), contentAlignment = Alignment.Center) { StreakFlame(24.dp) }
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.streak_repair_card_title, old, old), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                Text(stringResource(R.string.streak_repair_card_sub), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = BrownSoft)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(timeLeftText(millisLeft), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, textAlign = TextAlign.End)
                Text(stringResource(R.string.streak_time_left_label), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = BrownSoft)
            }
        }
        if (!hasPlus) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PeachSoft).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlusPill(stringResource(R.string.plus_pill), fontSize = 12)
                Text(
                    stringResource(R.string.streak_repair_plus_note),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownMuted, modifier = Modifier.weight(1f),
                )
            }
        }
        PeachButton(
            stringResource(if (hasPlus) R.string.streak_repair_cta else R.string.streak_repair_plus_cta),
            onClick = { hideThen(if (hasPlus) onMakeUp else onPlus) },
            modifier = Modifier.padding(top = 4.dp),
        )
        GhostButton(stringResource(R.string.streak_repair_restart), { hideThen(onRestart) }, Modifier.align(Alignment.CenterHorizontally))
    }
}
