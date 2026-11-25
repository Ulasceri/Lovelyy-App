package com.lovelyy.app.ui.cat

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatLevels
import com.lovelyy.app.data.CatMood
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.CareStreak
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.lovelyyApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/** The whole cat block of Home: the living cat, its needs, the care buttons and the streak. */
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

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CatStage(
            animator = animator,
            sleepingPoster = ui?.levels?.sleeping == true,
            name = ui?.doc?.name,
            streak = ui?.doc?.streak?.current(LocalDate.now()) ?: 0,
            banner = banner,
            onNameClick = { naming = true },
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
        )

        ui?.let { NeedsRow(it.levels) }

        CareButtons(
            sleeping = ui?.levels?.sleeping == true,
            enabled = ui != null,
            onFeed = { act(CatActionType.Feed) },
            onWater = { act(CatActionType.Water) },
            onPlay = { act(CatActionType.Play) },
            onSleepToggle = { act(if (ui?.levels?.sleeping == true) CatActionType.Wake else CatActionType.Sleep) },
        )

        ui?.let { u ->
            StreakCard(
                streak = u.doc.streak,
                lastAction = u.doc.lastAction,
                me = me,
                partnerUid = partner?.uid,
                partnerName = partnerName,
                describe = texts,
            )
        }
    }

    // First time: the couple names their cat together.
    val needsName = ui != null && ui?.doc?.name.isNullOrBlank()
    if (naming || needsName) {
        NameCatDialog(
            initial = ui?.doc?.name.orEmpty(),
            canDismiss = !needsName,
            onDismiss = { naming = false },
            onSave = { n ->
                naming = false
                vm.act(me, members, CatActionType.Name, name = n)
                animator.play(CatClip.Happy)
            },
        )
    }
}

private fun confirmHaptic() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP

/** The animation that shows an action. Sleep/Wake are shown by the mood itself. */
private fun CatActionType.clip(): CatClip? = when (this) {
    CatActionType.Feed -> CatClip.Eating
    CatActionType.Water -> CatClip.Drinking
    CatActionType.Play -> CatClip.Playing
    CatActionType.Pet -> CatClip.Petted
    CatActionType.Name -> CatClip.Happy
    CatActionType.Sleep, CatActionType.Wake -> null
}

// ---------- the stage ----------

private data class Heart(val id: Long, val at: Offset, val drift: Float, val size: Float)

@Composable
private fun CatStage(
    animator: CatAnimator,
    sleepingPoster: Boolean,
    name: String?,
    streak: Int,
    banner: String?,
    onNameClick: () -> Unit,
    onTap: () -> Unit,
    onPetting: (Boolean) -> Unit,
    onStroke: () -> Unit,
    onPetDone: (Int) -> Unit,
) {
    val hearts = remember { mutableStateListOf<Heart>() }
    val density = LocalDensity.current
    val strokePx = with(density) { STROKE_DP.dp.toPx() }
    val slopPx = with(density) { 10.dp.toPx() }
    val tap by rememberUpdatedState(onTap)
    val petting by rememberUpdatedState(onPetting)
    val stroke by rememberUpdatedState(onStroke)
    val petDone by rememberUpdatedState(onPetDone)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(STAGE_RATIO)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
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
        // Videos are square; the stage is a bit wider than tall, so crop top and bottom evenly.
        Box(Modifier.requiredSize(side, side)) {
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

        // Top: name tag and streak.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .zIndex(3f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chip(onClick = onNameClick) {
                Text(name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cat_unnamed), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            if (streak > 0) {
                Chip {
                    Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.cat_streak_short, streak), style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        // Bottom: what just happened ("Deniz besledi 💛").
        AnimatedVisibility(
            visible = banner != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .zIndex(3f),
        ) {
            var last by remember { mutableStateOf("") }
            if (banner != null) last = banner
            Text(
                last,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
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

@Composable
private fun Chip(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

// ---------- needs ----------

@Composable
private fun NeedsRow(levels: CatLevels) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NeedMeter(Icons.Filled.Restaurant, stringResource(R.string.need_hunger), levels.hunger, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        NeedMeter(Icons.Filled.WaterDrop, stringResource(R.string.need_thirst), levels.thirst, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        NeedMeter(Icons.Filled.Bolt, stringResource(R.string.need_energy), levels.energy, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        NeedMeter(Icons.Filled.Favorite, stringResource(R.string.need_love), levels.love, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}

@Composable
private fun NeedMeter(icon: ImageVector, label: String, value: Double, color: Color, modifier: Modifier) {
    val fraction by animateFloatAsState(
        (value / 100.0).toFloat().coerceIn(0f, 1f),
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "need",
    )
    val low = value < CatRules.LOW
    // A low need gently pulses so it's noticed without being alarming.
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, if (low) 0.45f else 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
    )
    val barColor = if (low) MaterialTheme.colorScheme.error.copy(alpha = 0.75f) else color
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp).graphicsLayer { alpha = pulse }, tint = if (low) barColor else MaterialTheme.colorScheme.tertiary)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// ---------- care buttons ----------

@Composable
private fun CareButtons(
    sleeping: Boolean,
    enabled: Boolean,
    onFeed: () -> Unit,
    onWater: () -> Unit,
    onPlay: () -> Unit,
    onSleepToggle: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CareButton(Icons.Filled.Restaurant, stringResource(R.string.cat_feed), enabled && !sleeping, MaterialTheme.colorScheme.primaryContainer, onFeed, Modifier.weight(1f))
        CareButton(Icons.Filled.WaterDrop, stringResource(R.string.cat_water), enabled && !sleeping, MaterialTheme.colorScheme.secondaryContainer, onWater, Modifier.weight(1f))
        CareButton(Icons.Filled.SportsBaseball, stringResource(R.string.cat_play), enabled && !sleeping, MaterialTheme.colorScheme.primaryContainer, onPlay, Modifier.weight(1f))
        CareButton(
            if (sleeping) Icons.Filled.WbSunny else Icons.Filled.Bedtime,
            stringResource(if (sleeping) R.string.cat_wake else R.string.cat_sleep),
            enabled, MaterialTheme.colorScheme.secondaryContainer, onSleepToggle, Modifier.weight(1f),
        )
    }
}

@Composable
private fun CareButton(icon: ImageVector, label: String, enabled: Boolean, container: Color, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press",
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.45f, label = "enabled")
    Column(
        modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.tertiary) }
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

// ---------- streak + last action ----------

@Composable
private fun StreakCard(
    streak: CareStreak,
    lastAction: CatAction?,
    me: UserProfile,
    partnerUid: String?,
    partnerName: String,
    describe: (CatAction) -> String,
) {
    val today = LocalDate.now()
    val count = streak.current(today)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.tertiary) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (count > 0) stringResource(R.string.cat_streak_title, count) else stringResource(R.string.cat_streak_none),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.cat_streak_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CareTick(stringResource(R.string.cat_you), streak.caredToday(me.uid, today))
            CareTick(partnerName, partnerUid != null && streak.caredToday(partnerUid, today))
        }
        if (lastAction != null) {
            Text(
                describe(lastAction) + " · " + relativeTime(lastAction.at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CareTick(name: String, done: Boolean) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant, label = "tick",
    )
    Text(
        (if (done) "✓ " else "○ ") + name,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun relativeTime(at: Long): String {
    val minutes = ((System.currentTimeMillis() - at) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.time_hours, minutes / 60)
        else -> stringResource(R.string.time_days, minutes / (24 * 60))
    }
}

/** "Deniz besledi", "Sen oynadın"... */
@Composable
private fun actionTexts(partnerName: String, myUid: String): (CatAction) -> String {
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
private fun refusalTexts(catName: String?): (CatRules.Refusal) -> String {
    val context = LocalContext.current
    val name = catName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.cat_generic)
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
private fun NameCatDialog(initial: String, canDismiss: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
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
            TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = if (canDismiss) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        } else null,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.background,
    )
}

private const val STAGE_RATIO = 1.12f
private const val STROKE_DP = 70
