package com.lovelyy.app.ui.cat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.lovelyy.app.data.CareRewards.Accessory
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.drawSvg
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.NeedOk
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

private val Track = Color(0xFFF3EAE3)
private val ButtonIdle = Color(0xFFF6EEE8)
private val StreakGreen = Color(0xFF3E9C82)
private val LockedRing = Color(0xFFB8A69D)

private const val SHIRT_D = "M8 4l4 2 4-2 5 4-3 3-2-1v10H8V10l-2 1-3-3z"
private const val BOWL_D = "M3 12h18a9 6 0 0 1-18 0z M8.5 9.5c0-1 .8-2 2-2M13 9c0-1 .8-2 2-2"
private const val DROP_D = "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"
private const val MOON_D = "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"
private const val FLAME_D = "M12 21c-4 0-6-2.7-6-6 0-4 4-6 4-10 3 2 4 4 4 6 1-1 1.5-2 1.5-3 2 2 2.5 4.5 2.5 7 0 3.3-2 6-6 6z"
private const val GIFT_D = "M4 10h16v10H4z M3 7h18v3H3zM12 7v13M12 7c-1.5-3-5-3-5-1s3 1 5 1zm0 0c1.5-3 5-3 5-1s-3 1-5 1z"
private const val LOCK_D = "M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 17.5v-4A2.5 2.5 0 0 1 7.5 11z M8 11V8a4 4 0 0 1 8 0v3"

/** Kedi bakımı (approved design 14): the cat up close, its four needs with a button each, and the care streak. */
@Composable
fun CatCareScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit, onAccessories: () -> Unit) {
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
    var naming by remember { mutableStateOf(false) }

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

    val catName = ui?.doc?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cat_generic)
    val today = LocalDate.now()
    val todayLine = ui?.doc?.recent?.let { todayLineText(it, me.uid, partnerName) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(catName, onBack) {
            RoundSvgButton(SHIRT_D, stringResource(R.string.cc_accessories), onAccessories)
        }

        // Hero: the living cat. The stage is short, so keep the head and ears in view.
        val heroShape = RoundedCornerShape(28.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .softShadow(heroShape, 14.dp)
                .clip(heroShape)
                .background(CatBg),
        ) {
            CatStage(
                animator = animator,
                sleepingPoster = levels?.sleeping == true,
                name = ui?.doc?.name,
                streak = 0,
                bubble = mood?.let { stringResource(it.bubbleRes()) },
                banner = banner,
                onNameClick = { naming = true },
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
                stageModifier = Modifier.height(196.dp),
                cropBias = 0.3f,
                showChips = false,
                wearing = ui?.doc?.let { CareRewards.worn(it.wearing, it.streak.best) },
                bubbleTop = 16.dp,
                bubbleEnd = 16.dp,
                footer = todayLine,
            )
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

        val streak = ui?.doc?.streak
        StreakCard(
            current = streak?.current(today) ?: 0,
            best = streak?.best ?: 0,
            wearing = ui?.doc?.let { CareRewards.worn(it.wearing, it.streak.best) },
            onWear = { a -> vm.wear(a) },
            onLocked = { a ->
                banner = context.getString(R.string.cc_locked_hint, context.getString(accessoryNameRes(a)), a.day)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            },
        )
    }

    if (naming) {
        NameCatDialog(
            initial = ui?.doc?.name.orEmpty(),
            canDismiss = true,
            onDismiss = { naming = false },
            onSave = { n ->
                naming = false
                vm.act(me, members, CatActionType.Name, name = n)
                animator.play(CatClip.Happy)
            },
        )
    }
}

/** "Bugün: sen sevdin · Elif su verdi" — shared by Yuva and Kedi bakımı so both show the same line. */
@Composable
internal fun todayLineText(recent: List<CatAction>, myUid: String, partnerName: String): String {
    val latest = CareRewards.todayLatest(recent, myUid, LocalDate.now())
    return if (latest.isEmpty()) stringResource(R.string.cc_today_none)
    else stringResource(R.string.cc_today, latest.map { todayText(it, myUid, partnerName) }.joinToString(" · "))
}

@Composable
private fun todayText(a: CatAction, myUid: String, partnerName: String): String {
    val mine = a.uid == myUid
    val who = a.name.ifBlank { partnerName }
    return when (a.type) {
        CatActionType.Feed -> if (mine) stringResource(R.string.cc_me_feed) else stringResource(R.string.cc_feed, who)
        CatActionType.Water -> if (mine) stringResource(R.string.cc_me_water) else stringResource(R.string.cc_water, who)
        CatActionType.Play -> if (mine) stringResource(R.string.cc_me_play) else stringResource(R.string.cc_play, who)
        CatActionType.Pet -> if (mine) stringResource(R.string.cc_me_pet) else stringResource(R.string.cc_pet, who)
        CatActionType.Sleep -> if (mine) stringResource(R.string.cc_me_sleep) else stringResource(R.string.cc_sleep, who)
        CatActionType.Wake -> if (mine) stringResource(R.string.cc_me_wake) else stringResource(R.string.cc_wake, who)
        CatActionType.Name -> ""
    }
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
    Column(
        modifier.softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$label %$pct" },
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

@Composable
private fun StreakCard(current: Int, best: Int, wearing: Accessory?, onWear: (Accessory?) -> Unit, onLocked: (Accessory) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val next = CareRewards.next(current, best)
    Column(
        Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val flame = remember { svgPath(FLAME_D) }
            Canvas(Modifier.size(24.dp)) {
                drawSvg(flame, 24f, Peach)
                drawSvg(flame, 24f, Color(0xFFE07A45), strokeWidth = 1.6f)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cc_streak_title, current), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                Text(
                    if (next != null) stringResource(R.string.cc_streak_next, accessoryName(next), next.day - current)
                    else stringResource(R.string.cc_streak_all),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (next != null) {
                Text(
                    stringResource(R.string.cc_streak_chip, current, next.day),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(PeachSoft).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        val progress by animateFloatAsState(CareRewards.progress(current, best), label = "streak")
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Track)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(StreakGreen))
        }
        if (Accessory.entries.any { CareRewards.unlocked(it, best) }) {
            Text(stringResource(R.string.cc_wear_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Accessory.entries.forEach { a ->
                AccessoryBadge(a, CareRewards.unlocked(a, best), a == wearing, Modifier.weight(1f)) {
                    if (CareRewards.unlocked(a, best)) onWear(if (a == wearing) null else a) else onLocked(a)
                }
            }
        }
    }
}

@Composable
private fun AccessoryBadge(a: Accessory, unlocked: Boolean, worn: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val name = accessoryName(a)
    val label = stringResource(if (worn) R.string.cc_worn else if (unlocked) R.string.cc_unlocked else R.string.cc_locked, name) + ", " + stringResource(R.string.cc_day, a.day)
    Column(
        modifier.clearAndSetSemantics { contentDescription = label }.bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (unlocked) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(if (worn) Peach else Mint)
                    .then(if (worn) Modifier.border(3.dp, Brown, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                SvgStrokeIcon(if (worn) com.lovelyy.app.ui.components.Kit.CHECK_D else GIFT_D, Brown, Modifier.size(22.dp))
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(48.dp)) {
                    val w = 2.dp.toPx()
                    drawCircle(
                        LockedRing, radius = size.minDimension / 2 - w / 2,
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                }
                SvgStrokeIcon(LOCK_D, BrownSoft, Modifier.size(18.dp))
            }
        }
        Text(name, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, maxLines = 1)
        Text(stringResource(R.string.cc_day, a.day), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = BrownSoft)
    }
}

@Composable
private fun accessoryName(a: Accessory): String = stringResource(accessoryNameRes(a))

internal fun accessoryNameRes(a: Accessory): Int = when (a) {
    Accessory.Kurdele -> R.string.cc_acc_kurdele
    Accessory.Papyon -> R.string.cc_acc_papyon
    Accessory.Sapka -> R.string.cc_acc_sapka
    Accessory.Gozluk -> R.string.cc_acc_gozluk
}
