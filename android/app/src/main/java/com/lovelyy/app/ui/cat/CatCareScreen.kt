package com.lovelyy.app.ui.cat

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import com.lovelyy.app.data.CoupleClock
import com.lovelyy.app.data.StreakExplainerSource
import com.lovelyy.app.text.AppLocale
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.profile.CHEVRON_D
import com.lovelyy.app.ui.theme.CheckInk
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.CareRewards
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.displayCatName
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.NeedOk
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

private val Track = Color(0xFFF3EAE3)
private val ButtonIdle = Color(0xFFF6EEE8)

private const val BOWL_D = "M3 12h18a9 6 0 0 1-18 0z M8.5 9.5c0-1 .8-2 2-2M13 9c0-1 .8-2 2-2"
private const val DROP_D = "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"
private const val MOON_D = "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"

/**
 * Kedi bakımı (approved board "14 · Kedi bakımı v2"): the cat up close with its speech bubble below the video,
 * its four needs with a button each, and the care streak card (who cared today; tap = "Seri nedir?").
 */
@Composable
fun CatCareScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val members = couple?.members.orEmpty()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val animator = remember { CatAnimator(context, scope) }
    val view = LocalView.current
    var banner by remember { mutableStateOf<String?>(null) }
    var explainer by remember { mutableStateOf<StreakExplainerSource?>(null) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_STOP -> animator.pause()
                Lifecycle.Event.ON_START -> animator.resume()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            animator.release()
        }
    }

    val levels = ui?.levels
    val mood = levels?.mood
    LaunchedEffect(mood) {
        if (mood == null) return@LaunchedEffect
        animator.start(mood)
        animator.setMood(mood)
    }

    val texts = actionTexts(partnerName, me.uid)
    val refusalTexts = refusalTexts(ui?.doc?.name)
    LaunchedEffect(vm) {
        vm.oneTimeEvents.collect { e ->
            when (e) {
                is CatEvent.PartnerAction -> {
                    e.action.type.clip()?.let(animator::play)
                    banner = texts(e.action)
                }
                is CatEvent.Refused -> banner = refusalTexts(e.reason)
                CatEvent.Failed -> banner = context.getString(R.string.cat_error)
                CatEvent.FirstStreak -> explainer = StreakExplainerSource.Auto
                is CatEvent.DayOffTaken -> banner = context.resources.getQuantityString(R.plurals.streak_off_done, e.days, e.days)
                is CatEvent.StreakMadeUp -> banner = context.resources.getQuantityString(R.plurals.streak_repair_done, e.days, e.days)
                CatEvent.NudgeSent -> banner = context.getString(R.string.nudge_sent, partnerName)
                CatEvent.NudgeLimited -> banner = context.getString(R.string.signal_limit)
            }
        }
    }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(3200)
            banner = null
        }
    }

    fun act(type: CatActionType) {
        val refusal = vm.canDo(type)
        if (refusal != null) {
            banner = refusalTexts(refusal)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return
        }
        type.clip()?.let(animator::play)
        view.performHapticFeedback(confirmHaptic())
        vm.act(me, members, type)
    }

    val catName = displayCatName(ui?.doc?.name)
    val today = streakToday()
    val zone by CoupleClock.zone.collectAsStateWithLifecycle()
    val latestToday = ui?.doc?.recent?.let { CareRewards.todayLatest(it, me.uid, today, zone) }.orEmpty()
    val streak = ui?.doc?.streak
    val currentStreak = streak?.current(today) ?: 0
    val myName = me.displayName.ifBlank { stringResource(R.string.cat_you) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(catName, onBack)

        // Hero: the living cat, nothing drawn over it; the bubble (or a banner) sits below the video.
        val heroShape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(heroShape, 14.dp)
                .clip(heroShape)
                .background(CatBg),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                CatStage(
                    animator = animator,
                    sleepingPoster = levels?.sleeping == true,
                    catName = catName,
                    onTap = {
                        val clip = animator.currentClip
                        if (levels?.sleeping != true && clip?.isBridge != true) {
                            animator.play(CatClip.Surprised)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    },
                    onPetting = { animator.setPetting(it) },
                    onStroke = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
                    onPetDone = { strokes -> if (strokes > 0) vm.act(me, members, CatActionType.Pet, strokes) },
                    // Same video size and crop as the Yuva card, so the whole cat stays in view.
                    modifier = Modifier.height(CatCardGeometry.videoHeightDp(maxWidth.value).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                )
            }
            CatSpeech(bubble = mood?.let { stringResource(it.bubbleRes()) }, banner = banner)
            Spacer(Modifier.height(10.dp))
        }

        // 2×2 needs: each with its own button; the most needed one is peach.
        val sleeping = levels?.sleeping == true
        val enabled = ui != null
        val highlight = levels?.let(::mostNeeded)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeedCard(BOWL_D, stringResource(R.string.need_hunger), levels?.hunger, stringResource(R.string.cat_feed),
                enabled, highlight == CatActionType.Feed, Modifier.weight(1f), dimmed = sleeping) { act(CatActionType.Feed) }
            NeedCard(DROP_D, stringResource(R.string.need_thirst), levels?.thirst, stringResource(R.string.cat_water),
                enabled, highlight == CatActionType.Water, Modifier.weight(1f), dimmed = sleeping) { act(CatActionType.Water) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeedCard(MOON_D, stringResource(R.string.need_energy), levels?.energy,
                stringResource(if (sleeping) R.string.cat_wake else R.string.cat_sleep),
                enabled, highlight == CatActionType.Sleep, Modifier.weight(1f)) { act(if (sleeping) CatActionType.Wake else CatActionType.Sleep) }
            NeedCard(HEART_D, stringResource(R.string.need_love), levels?.love, stringResource(R.string.cat_play),
                enabled, highlight == CatActionType.Play, Modifier.weight(1f), dimmed = sleeping) { act(CatActionType.Play) }
        }

        StreakCard(
            current = currentStreak,
            best = streak?.best ?: 0,
            myName = myName,
            partnerName = partnerName,
            myText = latestToday.firstOrNull { it.uid == me.uid }?.let { todayText(it, me.uid, partnerName) }
                ?: stringResource(R.string.cc_me_none),
            partnerText = latestToday.firstOrNull { it.uid != me.uid }?.let { todayText(it, me.uid, partnerName) }
                ?: stringResource(R.string.cc_partner_none, partnerName),
            meDone = streak?.caredToday(me.uid, today) == true,
            partnerDone = partner?.uid?.let { streak?.caredToday(it, today) } == true,
            onClick = {
                vm.streakExplainerOpened(StreakExplainerSource.Card)
                explainer = StreakExplainerSource.Card
            },
        )

        // P5a: after 20:00 while today's care is missing (with the P5b day-off sheet).
        StreakDangerSection(vm, me, partner, catName)
    }

    explainer?.let {
        StreakExplainerSheet(
            catName = catName,
            myName = myName,
            partnerName = partnerName,
            streak = currentStreak,
            bothCaredToday = streak?.lastCompleteDay == today.toString(),
            onDismiss = { explainer = null },
        )
    }
}

/** One person's latest care today, as on the streak card: "Sen sevdin", "Elif su verdi". */
@Composable
private fun todayText(a: CatAction, myUid: String, partnerName: String): String {
    val mine = a.uid == myUid
    val who = a.name.ifBlank { partnerName }
    val text = when (a.type) {
        CatActionType.Feed -> if (mine) stringResource(R.string.cc_me_feed) else stringResource(R.string.cc_feed, who)
        CatActionType.Water -> if (mine) stringResource(R.string.cc_me_water) else stringResource(R.string.cc_water, who)
        CatActionType.Play -> if (mine) stringResource(R.string.cc_me_play) else stringResource(R.string.cc_play, who)
        CatActionType.Pet -> if (mine) stringResource(R.string.cc_me_pet) else stringResource(R.string.cc_pet, who)
        CatActionType.Sleep -> if (mine) stringResource(R.string.cc_me_sleep) else stringResource(R.string.cc_sleep, who)
        CatActionType.Wake -> if (mine) stringResource(R.string.cc_me_wake) else stringResource(R.string.cc_wake, who)
        CatActionType.Name -> ""
    }
    return text.replaceFirstChar { it.titlecase(AppLocale.current()) }
}

@Composable
private fun NeedCard(
    icon: String,
    label: String,
    value: Double?,
    action: String,
    enabled: Boolean,
    highlighted: Boolean,
    modifier: Modifier,
    dimmed: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val pct = (value ?: 0.0).roundToInt().coerceIn(0, 100)
    val fraction by animateFloatAsState(
        pct / 100f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "need",
    )
    val low = value != null && value < CatRules.LOW
    val needDescription = "$label ${stringResource(R.string.cc_percent, pct)}"
    Column(
        modifier.softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clearAndSetSemantics { contentDescription = needDescription },
        ) {
            SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
            Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                if (value == null) "–" else stringResource(R.string.cc_percent, pct),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
            )
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
            Box(
                Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (low) NeedLow else NeedOk),
            )
        }
        val bg by animateColorAsState(if (highlighted) Peach else ButtonIdle, label = "bg")
        val alpha by animateFloatAsState(if (enabled && !dimmed) 1f else 0.45f, label = "enabled")
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(RoundedCornerShape(22.dp))
                .background(bg)
                .semantics { role = Role.Button }
                .bounceClick(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(action, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
        }
    }
}

/**
 * "N gün bakım serisi" with the best streak and, below, who cared today (board 14 v2: this line used to sit on the
 * video). The whole card opens "Seri nedir?".
 */
@Composable
private fun StreakCard(
    current: Int,
    best: Int,
    myName: String,
    partnerName: String,
    myText: String,
    partnerText: String,
    meDone: Boolean,
    partnerDone: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val label = pluralStringResource(R.plurals.cc_streak_card_label, current, current)
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(CreamCard)
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "$label $myText, $partnerText"
                role = Role.Button
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StreakFlame(24.dp)
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.cc_streak_title, current, current), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                Text(
                    pluralStringResource(R.plurals.cc_streak_best, best, best),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            SvgStrokeIcon(CHEVRON_D, BrownSoft, Modifier.size(16.dp))
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cream).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CaredToday(myName, Peach, myText, meDone, Modifier.weight(1f))
            CaredToday(partnerName, Mint, partnerText, partnerDone, Modifier.weight(1f))
        }
    }
}

/** One half of the "today" row: avatar, what they did, a check once they count for today's streak. */
@Composable
private fun CaredToday(name: String, color: Color, text: String, done: Boolean, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InitialAvatar(name, color, 26.dp, 11.sp)
        Text(
            text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
            lineHeight = 15.sp,
            // Two lines so "Sen henüz bakmadın" fits on 360dp phones instead of being cut off.
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        if (done) SvgStrokeIcon(Kit.CHECK_D, CheckInk, Modifier.size(14.dp), strokeWidth = 2.6f)
    }
}
