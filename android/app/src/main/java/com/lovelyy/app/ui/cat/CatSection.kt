package com.lovelyy.app.ui.cat

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.lovelyy.app.data.StreakExplainerSource
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.theme.CatStripEnd
import com.lovelyy.app.ui.theme.CatStripStart
import android.view.HapticFeedbackConstants
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.displayCatName
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatLevels
import com.lovelyy.app.data.CatMood
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.CareStreak
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.NeedOk
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The whole cat block of Home (approved board "V2 · Anasayfa kedi kartı v2"): a strip with the name and the
 * streak, the living cat with nothing drawn over it, the speech bubble below it, the needs, the care buttons and
 * the streak status. The streak chip opens "Seri nedir?" ([StreakExplainerSheet]).
 */
@Composable
fun CatSection(me: UserProfile, couple: Couple?, partner: UserProfile?, modifier: Modifier = Modifier) {
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
    var explainer by remember { mutableStateOf<StreakExplainerSource?>(null) }

    // Pause the videos when the app is in the background; free them when Home goes away.
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

    val mood = ui?.levels?.mood
    LaunchedEffect(mood) {
        if (mood == null) return@LaunchedEffect
        animator.start(mood) // no-op after the first time
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

    fun act(type: CatActionType, strokes: Int = 0) {
        val refusal = vm.canDo(type)
        if (refusal != null) {
            banner = refusalTexts(refusal)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return
        }
        type.clip()?.let(animator::play)
        view.performHapticFeedback(confirmHaptic())
        vm.act(me, members, type, strokes)
    }

    val today = streakToday()
    val streak = ui?.doc?.streak?.current(today) ?: 0
    Column(modifier) {
        val heroShape = RoundedCornerShape(28.dp)
        // No clip on the card: the strip's buttons reach above it for a 48dp touch target. The video clips itself.
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(heroShape, 14.dp)
                .background(CatBg, heroShape),
        ) {
            CatStrip(
                name = ui?.doc?.name,
                streak = streak,
                onNameClick = { naming = true },
                onStreakClick = {
                    vm.streakExplainerOpened(StreakExplainerSource.Chip)
                    explainer = StreakExplainerSource.Chip
                },
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                CatStage(
                    animator = animator,
                    sleepingPoster = ui?.levels?.sleeping == true,
                    catName = displayCatName(ui?.doc?.name),
                    onTap = {
                        val clip = animator.currentClip
                        if (ui?.levels?.sleeping != true && clip?.isBridge != true) {
                            animator.play(CatClip.Surprised)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    },
                    onPetting = { animator.setPetting(it) },
                    onStroke = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
                    onPetDone = { strokes -> if (strokes > 0) vm.act(me, members, CatActionType.Pet, strokes) },
                    modifier = Modifier.height(CatCardGeometry.videoHeightDp(maxWidth.value).dp),
                )
            }
            CatSpeech(bubble = mood?.let { stringResource(it.bubbleRes()) }, banner = banner)
            NeedsRow(ui?.levels)
        }

        CareButtons(
            sleeping = ui?.levels?.sleeping == true,
            enabled = ui != null,
            highlight = ui?.levels?.let(::mostNeeded),
            onFeed = { act(CatActionType.Feed) },
            onWater = { act(CatActionType.Water) },
            onPlay = { act(CatActionType.Play) },
            onSleepToggle = { act(if (ui?.levels?.sleeping == true) CatActionType.Wake else CatActionType.Sleep) },
            modifier = Modifier.padding(top = 18.dp),
        )

        ui?.let { u ->
            StatusLine(
                streak = u.doc.streak,
                me = me,
                partnerUid = partner?.uid,
                partnerName = partnerName,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }

    explainer?.let {
        StreakExplainerSheet(
            catName = displayCatName(ui?.doc?.name),
            myName = me.displayName.ifBlank { stringResource(R.string.cat_you) },
            partnerName = partnerName,
            streak = streak,
            bothCaredToday = ui?.doc?.streak?.lastCompleteDay == today.toString(),
            onDismiss = { explainer = null },
        )
    }

    // Renaming is optional; the cat starts out as DEFAULT_CAT_NAME.
    if (naming) {
        NameCatDialog(
            initial = displayCatName(ui?.doc?.name),
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

internal fun confirmHaptic() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP

/** The animation that shows an action. Sleep/Wake are shown by the mood itself. */
internal fun CatActionType.clip(): CatClip? = when (this) {
    CatActionType.Feed -> CatClip.Eating
    CatActionType.Water -> CatClip.Drinking
    CatActionType.Play -> CatClip.Playing
    CatActionType.Pet -> CatClip.Petted
    CatActionType.Name -> CatClip.Happy
    CatActionType.Sleep, CatActionType.Wake -> null
}

// ---------- the stage ----------

private data class Heart(val id: Long, val at: Offset, val drift: Float, val size: Float)

/**
 * The living cat: the square video cropped to [modifier]'s size, tap = surprise, stroke = petting with hearts.
 * Nothing else is drawn over it (approved v2 boards); the name, streak, bubble and banners sit around it.
 */
@Composable
internal fun CatStage(
    animator: CatAnimator,
    sleepingPoster: Boolean,
    catName: String,
    onTap: () -> Unit,
    onPetting: (Boolean) -> Unit,
    onStroke: () -> Unit,
    onPetDone: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** The video's own clip: square on Home (the strip is above it), rounded on top on Kedi bakımı. */
    shape: Shape = RectangleShape,
    /** Which part of the square video stays visible when the stage is wider than tall (0.5 = middle). */
    cropBias: Float = 0.5f,
) {
    val hearts = remember { mutableStateListOf<Heart>() }
    val density = LocalDensity.current
    val strokePx = with(density) { STROKE_DP.dp.toPx() }
    val slopPx = with(density) { 10.dp.toPx() }
    val tap by rememberUpdatedState(onTap)
    val petting by rememberUpdatedState(onPetting)
    val stroke by rememberUpdatedState(onStroke)
    val petDone by rememberUpdatedState(onPetDone)
    val label = stringResource(R.string.cat_video_label, catName)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CatBg)
            .semantics { contentDescription = label }
            // Tap = surprise. Stroke = petting (hearts + purring + love).
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = 0f
                    var sinceStroke = 0f
                    var strokes = 0
                    var isPetting = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val d = change.positionChange().getDistance()
                        moved += d
                        change.consume() // the cat area doesn't scroll: strokes are for the cat
                        if (!isPetting && moved > slopPx) {
                            isPetting = true
                            petting(true)
                        }
                        if (isPetting) {
                            sinceStroke += d
                            if (sinceStroke > strokePx) {
                                sinceStroke = 0f
                                strokes++
                                stroke()
                                hearts += Heart(
                                    System.nanoTime(), change.position,
                                    drift = Random.nextFloat() * 2f - 1f,
                                    size = 28f + Random.nextFloat() * 16f,
                                )
                            }
                        }
                    }
                    if (isPetting) {
                        petting(false)
                        petDone(strokes)
                    } else {
                        tap()
                    }
                }
            },
    ) {
        val side = maxWidth
        // Videos are square; the stage is wider than tall, so crop top and bottom (evenly by default; the cat
        // sits in the middle of every clip, see CatCardGeometry).
        val shift = (side - maxHeight) * (0.5f - cropBias)
        Box(Modifier.requiredSize(side, side).offset(y = shift)) {
            if (!animator.started) {
                Image(
                    painter = painterResource(if (sleepingPoster) R.drawable.cat_sleep else R.drawable.cat_base),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            for (layer in 0..1) {
                AndroidView(
                    factory = { ctx -> TextureView(ctx).also { animator.bind(layer, it) } },
                    update = { it.alpha = animator.alpha(layer) },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(animator.z(layer)),
                )
            }
        }

        hearts.forEach { h -> key(h.id) { FloatingHeart(h) { hearts.remove(h) } } }
    }
}

// ---------- strip above the video ----------

/**
 * The strip above the video (board V2): the cat's name with a pencil (rename) and the streak chip ("Seri nedir?").
 * Both buttons get a 48dp touch target that reaches [TOUCH_REACH] above the strip, outside the card, so the
 * strip stays 36dp and the extra touch area never covers the cat.
 */
@Composable
private fun CatStrip(name: String?, streak: Int, onNameClick: () -> Unit, onStreakClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(STRIP_HEIGHT)
            .background(
                Brush.horizontalGradient(listOf(CatStripStart, CatStripEnd)),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val renameLabel = stringResource(R.string.cat_rename_label)
        Row(
            Modifier
                .weight(1f, fill = false)
                .touchTargetAbove()
                .widthIn(min = 48.dp)
                .bounceClick(onClick = onNameClick)
                .clearAndSetSemantics {
                    contentDescription = renameLabel
                    role = Role.Button
                }
                .padding(top = TOUCH_REACH),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cat_unnamed),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            SvgStrokeIcon(Kit.PENCIL_D, BrownSoft, Modifier.size(14.dp), strokeWidth = 2.2f)
        }
        Spacer(Modifier.width(8.dp))
        val chipLabel = pluralStringResource(R.plurals.cat_streak_chip_label, streak, streak)
        Box(
            Modifier
                .touchTargetAbove()
                .widthIn(min = 48.dp)
                .bounceClick(onClick = onStreakClick)
                .clearAndSetSemantics {
                    contentDescription = chipLabel
                    role = Role.Button
                }
                .padding(top = TOUCH_REACH),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CreamCard)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StreakFlame(16.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    pluralStringResource(R.plurals.cat_streak_chip, streak, streak),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                )
            }
        }
    }
}

/**
 * Measures the content [TOUCH_TARGET] tall but reports only the strip's height, placing the extra [TOUCH_REACH]
 * above the strip. The content pads itself down by [TOUCH_REACH] so it looks centred in the strip.
 */
private fun Modifier.touchTargetAbove(): Modifier = layout { measurable, constraints ->
    val target = TOUCH_TARGET.roundToPx()
    val visible = STRIP_HEIGHT.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = target, maxHeight = target))
    layout(placeable.width, visible) { placeable.placeRelative(0, visible - target) }
}

// ---------- speech below the video ----------

/**
 * Below the video (board V2): what the cat "says" (from its mood) in a bubble whose tail points up at the cat, or
 * for a few seconds what just happened ("Deniz besledi", "Mırmır uyuyor"), which takes the bubble's place.
 */
@Composable
internal fun CatSpeech(bubble: String?, banner: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(SPEECH_HEIGHT)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = banner?.let { Speech(it, isBanner = true) } ?: bubble?.let { Speech(it, isBanner = false) },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentAlignment = Alignment.TopCenter,
            label = "speech",
        ) { speech ->
            when {
                speech == null -> Spacer(Modifier.fillMaxWidth())
                speech.isBanner -> Box(Modifier.fillMaxWidth().height(SPEECH_HEIGHT), contentAlignment = Alignment.Center) {
                    Text(
                        speech.text,
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 15.sp, color = Brown,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CreamCard.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                }
                else -> SpeechBubble(speech.text)
            }
        }
    }
}

private data class Speech(val text: String, val isBanner: Boolean)

@Composable
private fun SpeechBubble(text: String) {
    // A gentle bob that never lets the tail touch the video.
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        0f, -2f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
    )
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier
            .padding(top = 7.dp)
            .graphicsLayer { translationY = bob.dp.toPx() },
        contentAlignment = Alignment.TopCenter,
    ) {
        // The tail: a rounded square turned 45°, half hidden under the bubble, pointing up at the cat.
        Box(
            Modifier
                .offset(y = (-5).dp)
                .size(12.dp)
                .graphicsLayer { rotationZ = 45f }
                .clip(RoundedCornerShape(3.dp))
                .background(CreamCard),
        )
        Text(
            text,
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp, color = Brown,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .softShadow(shape, 4.dp)
                .clip(shape)
                .background(CreamCard)
                .padding(horizontal = 14.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun FloatingHeart(h: Heart, onDone: () -> Unit) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        onDone()
    }
    val density = LocalDensity.current
    val rise = with(density) { 120.dp.toPx() }
    val sway = with(density) { 24.dp.toPx() }
    val sizePx = with(density) { h.size.dp.toPx() }
    Icon(
        Icons.Filled.Favorite,
        contentDescription = null,
        tint = Color(0xFFFF6F91),
        modifier = Modifier
            .zIndex(2f)
            .offset {
                IntOffset(
                    (h.at.x - sizePx / 2 + h.drift * sway * t.value).roundToInt(),
                    (h.at.y - sizePx / 2 - rise * t.value).roundToInt(),
                )
            }
            .size(h.size.dp)
            .graphicsLayer {
                val v = t.value
                alpha = if (v < 0.15f) v / 0.15f else 1f - (v - 0.15f) / 0.85f
                val s = 0.6f + 0.6f * minOf(1f, v * 3f)
                scaleX = s; scaleY = s
            },
    )
}

// ---------- needs ----------

@Composable
private fun NeedsRow(levels: CatLevels?) {
    Row(
        // Top 8dp (was 12): those 4dp went to the video, see CatCardGeometry.NEEDS_TRIM_DP.
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NeedMeter(stringResource(R.string.need_hunger), levels?.hunger, Modifier.weight(1f))
        NeedMeter(stringResource(R.string.need_thirst), levels?.thirst, Modifier.weight(1f))
        NeedMeter(stringResource(R.string.need_energy), levels?.energy, Modifier.weight(1f))
        NeedMeter(stringResource(R.string.need_love), levels?.love, Modifier.weight(1f))
    }
}

@Composable
private fun NeedMeter(label: String, value: Double?, modifier: Modifier) {
    val fraction by animateFloatAsState(
        ((value ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "need",
    )
    val low = value != null && value < CatRules.LOW
    // A low need gently pulses so it's noticed without being alarming.
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, if (low) 0.5f else 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, maxLines = 1)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .graphicsLayer { alpha = pulse }
                    .clip(CircleShape)
                    .background(if (low) NeedLow else NeedOk)
            )
        }
    }
}

/** The care button to highlight: the need that is lowest (if it's getting low). */
internal fun mostNeeded(l: CatLevels): CatActionType? {
    if (l.sleeping) return null
    val needs = listOf(
        CatActionType.Feed to l.hunger,
        CatActionType.Water to l.thirst,
        CatActionType.Play to l.love,
        CatActionType.Sleep to l.energy,
    )
    val (type, v) = needs.minBy { it.second }
    return if (v < CatRules.HAPPY) type else null
}

internal fun CatMood.bubbleRes(): Int = when (this) {
    CatMood.Sleeping -> R.string.bubble_sleeping
    CatMood.Hungry -> R.string.bubble_hungry
    CatMood.Thirsty -> R.string.bubble_thirsty
    CatMood.Sleepy -> R.string.bubble_sleepy
    CatMood.WantsLove -> R.string.bubble_love
    CatMood.Happy -> R.string.bubble_happy
    CatMood.Idle -> R.string.bubble_idle
}

// ---------- care buttons ----------

@Composable
private fun CareButtons(
    sleeping: Boolean,
    enabled: Boolean,
    highlight: CatActionType?,
    onFeed: () -> Unit,
    onWater: () -> Unit,
    onPlay: () -> Unit,
    onSleepToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CareButton(Icons.Filled.Restaurant, stringResource(R.string.cat_feed), enabled, highlight == CatActionType.Feed, onFeed, Modifier.weight(1f), dimmed = sleeping)
        CareButton(Icons.Filled.WaterDrop, stringResource(R.string.cat_water), enabled, highlight == CatActionType.Water, onWater, Modifier.weight(1f), dimmed = sleeping)
        CareButton(Icons.Filled.SportsBaseball, stringResource(R.string.cat_play), enabled, highlight == CatActionType.Play, onPlay, Modifier.weight(1f), dimmed = sleeping)
        CareButton(
            if (sleeping) Icons.Filled.WbSunny else Icons.Filled.Bedtime,
            stringResource(if (sleeping) R.string.cat_wake else R.string.cat_sleep),
            enabled, highlight == CatActionType.Sleep, onSleepToggle, Modifier.weight(1f),
        )
    }
}

@Composable
private fun CareButton(icon: ImageVector, label: String, enabled: Boolean, highlighted: Boolean, onClick: () -> Unit, modifier: Modifier, dimmed: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press",
    )
    val alpha by animateFloatAsState(if (enabled && !dimmed) 1f else 0.45f, label = "enabled")
    val bg by androidx.compose.animation.animateColorAsState(if (highlighted) Peach else CreamCard, label = "bg")
    Column(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .scale(scale)
                .softShadow(CircleShape, if (highlighted) 12.dp else 6.dp, if (highlighted) Peach else Brown)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(28.dp), tint = Brown) }
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
    }
}

// ---------- last action + streak ----------

/** "Elif 2 sa önce su verdi · Sen bugün ilgilendin ✓" */
@Composable
private fun StatusLine(
    streak: CareStreak,
    me: UserProfile,
    partnerUid: String?,
    partnerName: String,
    modifier: Modifier = Modifier,
) {
    val today = streakToday()
    val parts = buildList {
        val meDone = streak.caredToday(me.uid, today)
        val partnerDone = partnerUid != null && streak.caredToday(partnerUid, today)
        add(
            when {
                meDone && partnerDone -> stringResource(R.string.cat_both_cared)
                meDone -> stringResource(R.string.cat_waiting_partner, partnerName)
                else -> stringResource(R.string.cat_your_turn)
            },
        )
    }
    Text(
        parts.joinToString("\n"),
        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, color = BrownSoft,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** "Deniz besledi", "Sen oynadın"... */
@Composable
internal fun actionTexts(partnerName: String, myUid: String): (CatAction) -> String {
    val context = LocalContext.current
    return remember(partnerName, myUid) {
        { a: CatAction ->
            val mine = a.uid == myUid
            val who = if (mine) context.getString(R.string.cat_you) else a.name.ifBlank { partnerName }
            val res = when (a.type) {
                CatActionType.Feed -> if (mine) R.string.act_feed_me else R.string.act_feed
                CatActionType.Water -> if (mine) R.string.act_water_me else R.string.act_water
                CatActionType.Play -> if (mine) R.string.act_play_me else R.string.act_play
                CatActionType.Pet -> if (mine) R.string.act_pet_me else R.string.act_pet
                CatActionType.Sleep -> if (mine) R.string.act_sleep_me else R.string.act_sleep
                CatActionType.Wake -> if (mine) R.string.act_wake_me else R.string.act_wake
                CatActionType.Name -> if (mine) R.string.act_name_me else R.string.act_name
            }
            context.getString(res, who)
        }
    }
}

@Composable
internal fun refusalTexts(catName: String?): (CatRules.Refusal) -> String {
    val context = LocalContext.current
    val name = displayCatName(catName)
    return remember(name) {
        { r: CatRules.Refusal ->
            context.getString(
                when (r) {
                    CatRules.Refusal.Asleep -> R.string.refuse_asleep
                    CatRules.Refusal.TooTired -> R.string.refuse_tired
                    CatRules.Refusal.NotSleepy -> R.string.refuse_not_sleepy
                    CatRules.Refusal.Awake -> R.string.refuse_awake
                },
                name,
            )
        }
    }
}

// ---------- naming ----------

@Composable
internal fun NameCatDialog(initial: String, canDismiss: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        title = { Text(stringResource(R.string.cat_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.cat_name_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(20) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.cat_name_hint)) },
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank(), colors = ButtonDefaults.textButtonColors(contentColor = Brown)) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = if (canDismiss) {
            { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Brown)) { Text(stringResource(R.string.action_cancel)) } }
        } else null,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.background,
    )
}

private const val STROKE_DP = 70
private val STRIP_HEIGHT = CatCardGeometry.STRIP_DP.dp
private val SPEECH_HEIGHT = CatCardGeometry.SPEECH_DP.dp
private val TOUCH_TARGET = 48.dp
private val TOUCH_REACH = TOUCH_TARGET - STRIP_HEIGHT
