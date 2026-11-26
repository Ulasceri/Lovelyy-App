package com.lovelyy.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.BackButton
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.Pill
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.HeartInk
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintDeep
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand
import kotlin.math.PI
import kotlin.math.sin

private const val PAGES = 3

/** Where the partner's side starts below the top bar (progress bars + back/skip row). */
private val TOP_BAR = 84.dp

internal const val CHECK_D = "M5 12.5l4.5 4.5L19 7.5"
private const val BOWL_D = "M4 12h16a8 8 0 0 1-16 0zM9 8c0-1.5 1.5-1.5 1.5-3M13.5 8c0-1.5 1.5-1.5 1.5-3"
private const val PEN_D = "M4 20l4-1 11-11-3-3L5 16z"

// The two halves of the shared heart, drawn in the design's 390x500 stage.
private const val HEART_LEFT_D = "M195 326C150 296 118 258 118 222c0-30 25-46 46-41 15 4 26 15 31 30"
private const val HEART_RIGHT_D = "M195 211c5-15 16-26 31-30 21-5 46 11 46 41 0 36-32 74-77 104"

/**
 * The intro shown after "Başlayalım" on Karşılama (design boards A1-A3). It does not explain the
 * features, it lets you try them: each page has one tap whose result appears on the partner's side.
 * Trying is optional; "Devam" always works.
 *
 * [onDone]: skipped = left through "Atla"; tried = how many of the three demos were tapped.
 */
@Composable
fun IntroScreen(onBack: () -> Unit, onDone: (skipped: Boolean, tried: Int) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var tried0 by rememberSaveable { mutableStateOf(false) }
    var tried1 by rememberSaveable { mutableStateOf(false) }
    var tried2 by rememberSaveable { mutableStateOf(false) }
    val triedCount = listOf(tried0, tried1, tried2).count { it }
    val back: () -> Unit = { if (page > 0) page-- else onBack() }

    BackHandler(onBack = back)

    Column(Modifier.fillMaxSize().background(Cream)) {
        SplitStage(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { dir * it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 4 } + fadeOut())
                },
                modifier = Modifier.fillMaxSize(),
                label = "intro-stage",
            ) { p ->
                when (p) {
                    0 -> CatDemo(tried0) { tried0 = true }
                    1 -> HeartDemo(tried1) { tried1 = true }
                    else -> DrawDemo(tried2) { tried2 = true }
                }
            }
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp)
            ) {
                // Karşılama is the first of the four steps, so the bars start at two.
                StageProgress(filled = page + 2, total = PAGES + 1)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    BackButton(back)
                    if (page < PAGES - 1) {
                        Box(
                            Modifier
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(CreamCard.copy(alpha = 0.7f))
                                .bounceClick { onDone(true, triedCount) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.onb_skip), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
                        }
                    }
                }
            }
        }

        val tried = when (page) { 0 -> tried0; 1 -> tried1; else -> tried2 }
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp)) {
            Crossfade(tried, label = "hint") { done ->
                Pill(
                    stringResource(
                        when (page) {
                            0 -> if (done) R.string.onb_cat_hint_done else R.string.onb_cat_hint
                            1 -> if (done) R.string.onb_heart_hint_done else R.string.onb_heart_hint
                            else -> if (done) R.string.onb_draw_hint_done else R.string.onb_draw_hint
                        }
                    ),
                    background = if (done) PeachSoft else MintTint,
                    color = if (done) Brown else MintInk,
                )
            }
            Spacer(Modifier.height(10.dp))
            // The copy block keeps one height on every page (tallest case: a two-line title with a
            // two-line body), so the stage above does not resize between pages. Measured in sp so it
            // follows the font scale; the extra dp cover line-height rounding.
            val copyHeight = with(LocalDensity.current) { (35 * 2 + 24 * 2).sp.toDp() } + 16.dp
            Column(Modifier.heightIn(min = copyHeight)) {
                Text(
                    stringResource(when (page) { 0 -> R.string.onb_cat_title; 1 -> R.string.onb_heart_title; else -> R.string.onb_draw_title }),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp, color = Brown,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(when (page) { 0 -> R.string.onb_cat_body; 1 -> R.string.onb_heart_body; else -> R.string.onb_draw_body }),
                    fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = BrownSoft,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PeachButton(
            stringResource(if (page < PAGES - 1) R.string.onb_next else R.string.onb_create_account),
            onClick = { if (page < PAGES - 1) page++ else onDone(false, triedCount) },
            modifier = Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
private fun PersonBadge() = Icon(Icons.Rounded.Person, null, tint = Brown, modifier = Modifier.size(18.dp))

/** The dark "try me" pill on your side; turns into a white "done" pill once tapped. */
@Composable
private fun TryButton(tried: Boolean, label: String, doneLabel: String, iconD: String, onTry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (!tried) PulseRing(Brown, Modifier.matchParentSize())
        Row(
            Modifier
                .height(56.dp)
                .clip(CircleShape)
                .background(if (tried) CreamCard else Brown)
                .then(
                    if (tried) Modifier
                    else Modifier.semantics { role = Role.Button }.bounceClick(haptic = HapticFeedbackType.LongPress, onClick = onTry)
                )
                .padding(start = 16.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tried) SvgStrokeIcon(CHECK_D, MintInk, Modifier.size(20.dp), 2.6f)
            else SvgStrokeIcon(iconD, Cream, Modifier.size(20.dp), 2.2f)
            Text(if (tried) doneLabel else label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (tried) Brown else Cream)
        }
    }
}

// ---------- A1: two phones, one cat ----------

@Composable
private fun CatDemo(tried: Boolean, onTry: () -> Unit) {
    val top = statusBarTop()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val catSize = (maxHeight * 0.34f).coerceIn(110.dp, 170.dp)
        val food by animateFloatAsState(if (tried) 1f else 0.35f, tween(900), label = "food")
        val foodColor by animateColorAsState(if (tried) Peach else NeedLow, tween(900), label = "foodColor")

        SideChip(stringResource(R.string.onb_side_partner), Mint, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = top + TOP_BAR)) { PersonBadge() }
        PopIn(tried, Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = top + TOP_BAR + 42.dp), delayMs = 700) {
            StageBubble(stringResource(R.string.onb_cat_bubble), stringResource(R.string.onb_cat_bubble_sub), Modifier.widthIn(max = 230.dp)) {
                Badge(CatBg) {
                    Image(painterResource(R.drawable.cat_base), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Column(Modifier.align(Alignment.Center).offset(y = maxHeight * 0.12f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier
                        .size(catSize)
                        .softShadow(CircleShape, 14.dp)
                        .clip(CircleShape)
                        .background(CatBg)
                        .border(5.dp, CreamCard, CircleShape)
                ) {
                    Image(
                        painterResource(R.drawable.cat_base),
                        contentDescription = stringResource(R.string.welcome_cat_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (tried) {
                    RisingHeart(Modifier.offset(x = -catSize * 0.36f), 24.dp, HeartInk, 0)
                    RisingHeart(Modifier.offset(x = catSize * 0.38f, y = 10.dp), 18.dp, CreamCard, 500)
                    RisingHeart(Modifier.offset(y = (-8).dp), 20.dp, HeartInk, 1000)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .width(136.dp)
                    .softShadow(CircleShape, 4.dp)
                    .clip(CircleShape)
                    .background(CreamCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.onb_cat_food), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown)
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Sand)) {
                    Box(Modifier.fillMaxWidth(food).height(6.dp).clip(CircleShape).background(foodColor))
                }
            }
        }

        SideChip(stringResource(R.string.onb_side_me), Peach, Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp)) { PersonBadge() }
        TryButton(
            tried, stringResource(R.string.onb_cat_try), stringResource(R.string.onb_cat_done), BOWL_D, onTry,
            Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
        )
    }
}

/** A little heart that floats up and fades, forever (the design's `lv-rise`). */
@Composable
private fun RisingHeart(modifier: Modifier, size: Dp, color: Color, delayMs: Int) {
    val t by rememberInfiniteTransition(label = "rise").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart, StartOffset(delayMs)),
        label = "t",
    )
    Icon(
        Icons.Rounded.Favorite, null, tint = color,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = -70.dp.toPx() * t
                alpha = if (t < 0.2f) t / 0.2f else 1f - (t - 0.2f) / 0.8f
                scaleX = 0.7f + 0.4f * t
                scaleY = 0.7f + 0.4f * t
            },
    )
}

// ---------- A2: tap, they feel it ----------

@Composable
private fun HeartDemo(tried: Boolean, onTry: () -> Unit) {
    val top = statusBarTop()
    val haptic = LocalHapticFeedback.current
    val fly = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val arrived = fly.value >= 1f
    LaunchedEffect(tried) {
        if (tried) fly.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(arrived) {
        if (arrived) {
            // Your own phone buzzes too, so you feel what the partner would.
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(2) {
                shake.snapTo(0f)
                shake.animateTo(1f, tween(600, easing = LinearEasing))
            }
            shake.snapTo(0f)
        }
    }
    val beat by rememberInfiniteTransition(label = "beat").animateFloat(
        1f, 1.12f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s",
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        // From the centre of the heart button up to the centre of the partner's avatar.
        val flightPx = with(density) { (maxHeight - 86.dp - (top + TOP_BAR + 38.dp)).toPx() }

        SideChip(stringResource(R.string.onb_side_partner), Mint, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = top + TOP_BAR)) { PersonBadge() }

        Box(Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = top + TOP_BAR).size(76.dp)) {
            if (arrived) PulseRing(HeartInk, Modifier.matchParentSize())
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = sin(shake.value * 4f * PI.toFloat()) * 7f }
                    .softShadow(CircleShape, 8.dp)
                    .clip(CircleShape)
                    .background(Mint)
                    .border(4.dp, CreamCard, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Person, null, tint = Brown, modifier = Modifier.size(38.dp))
            }
        }

        PopIn(arrived, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = top + TOP_BAR + 48.dp)) {
            StageBubble(stringResource(R.string.onb_heart_bubble), stringResource(R.string.onb_heart_bubble_sub), Modifier.widthIn(max = 210.dp)) {
                Badge(Peach) { SvgIcon(HEART_D, Brown, Modifier.size(20.dp)) }
            }
        }

        Text(
            stringResource(R.string.onb_heart_seam),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = seamAngle(wPx, hPx) }
                .softShadow(CircleShape, 4.dp)
                .clip(CircleShape)
                .background(CreamCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        SideChip(stringResource(R.string.onb_side_me), Peach, Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp)) { PersonBadge() }

        if (tried && !arrived) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 72.dp, bottom = 68.dp)
                    .size(36.dp)
                    .graphicsLayer {
                        translationY = -fly.value * flightPx
                        val s = 1f + 0.4f * fly.value
                        scaleX = s
                        scaleY = s
                        alpha = if (fly.value < 0.85f) 1f else (1f - fly.value) / 0.15f
                    }
            ) { SvgIcon(HEART_D, HeartInk, Modifier.fillMaxSize()) }
        }

        val sendLabel = stringResource(R.string.onb_heart_try)
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 36.dp).size(100.dp)) {
            if (!tried) PulseRing(Brown, Modifier.matchParentSize())
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Peach)
                    .border(5.dp, CreamCard, CircleShape)
                    .then(
                        if (tried) Modifier
                        else Modifier
                            .semantics { role = Role.Button; contentDescription = sendLabel }
                            .bounceClick(haptic = HapticFeedbackType.LongPress, onClick = onTry)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (tried) SvgStrokeIcon(CHECK_D, Brown, Modifier.size(40.dp), 2.6f)
                else SvgIcon(HEART_D, Brown, Modifier.size(46.dp).graphicsLayer { scaleX = beat; scaleY = beat })
            }
        }
    }
}

// ---------- A3: half from you, half from them ----------

@Composable
private fun DrawDemo(tried: Boolean, onTry: () -> Unit) {
    val top = statusBarTop()
    val left = remember { svgPath(HEART_LEFT_D) }
    val right = remember { svgPath(HEART_RIGHT_D) }
    val leftMeasure = remember { PathMeasure().apply { setPath(left, false) } }
    val rightMeasure = remember { PathMeasure().apply { setPath(right, false) } }
    // The part drawn so far, cut out of each half (reused between frames).
    val leftDrawn = remember { Path() }
    val rightDrawn = remember { Path() }
    val mine = remember { Animatable(0f) }
    val theirs = remember { Animatable(0f) }
    LaunchedEffect(tried) {
        if (tried) {
            mine.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
            theirs.animateTo(1f, tween(1000, delayMillis = 100, easing = FastOutSlowInEasing))
        }
    }
    val guide = Brown.copy(alpha = 0.28f)

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val s = minOf(size.width / 390f, size.height / 500f)
            withTransform({
                // Put the heart's centre (195, 253 in the design) on the seam's centre.
                translate(size.width / 2 - 195f * s, size.height / 2 - 253f * s)
                scale(s, s, pivot = Offset.Zero)
            }) {
                val dots = Stroke(3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 9f)))
                drawPath(left, guide, style = dots)
                drawPath(right, guide, style = dots)
                val ink = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                if (mine.value > 0f) {
                    leftDrawn.reset()
                    leftMeasure.getSegment(0f, leftMeasure.length * mine.value, leftDrawn, true)
                    drawPath(leftDrawn, HeartInk, style = ink)
                }
                if (theirs.value > 0f) {
                    rightDrawn.reset()
                    rightMeasure.getSegment(0f, rightMeasure.length * theirs.value, rightDrawn, true)
                    drawPath(rightDrawn, MintDeep, style = ink)
                }
            }
        }

        SideChip(stringResource(R.string.onb_side_partner), Mint, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = top + TOP_BAR)) { PersonBadge() }
        PopIn(mine.value >= 1f, Modifier.align(Alignment.Center).offset(x = (-112).dp, y = (-40).dp)) {
            Pill(stringResource(R.string.onb_draw_me), color = HeartInk, fontSize = 12)
        }
        PopIn(theirs.value >= 1f, Modifier.align(Alignment.Center).offset(x = 112.dp, y = (-40).dp)) {
            Pill(stringResource(R.string.onb_draw_partner), color = MintInk, fontSize = 12)
        }
        PopIn(theirs.value >= 1f, Modifier.align(Alignment.Center).offset(y = 104.dp), delayMs = 300) {
            Pill(stringResource(R.string.onb_draw_saved))
        }
        SideChip(stringResource(R.string.onb_side_me), Peach, Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp)) { PersonBadge() }
        TryButton(
            tried, stringResource(R.string.onb_draw_try), stringResource(R.string.onb_draw_done), PEN_D, onTry,
            Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
        )
    }
}
