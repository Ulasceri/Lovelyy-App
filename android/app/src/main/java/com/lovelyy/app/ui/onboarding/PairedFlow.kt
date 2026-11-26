package com.lovelyy.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.location.CoarseLocation
import com.lovelyy.app.ui.components.BackButton
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.Pill
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.distance.DistanceViewModel
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.HeartInk
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay

private const val STEP_PAIRED = 0
private const val STEP_NOTIFICATIONS = 1
private const val STEP_LOCATION = 2

/**
 * Shown once, right after two people pair (design boards A4-A6): the "you are paired" moment, then a
 * primer for notifications and one for location. The system permission window only opens when the
 * main button is tapped; "Şimdi değil" never opens it. A permission that is already granted (or not
 * needed on this Android version) skips its primer.
 *
 * No names on these screens by design: it always says "Sevgilin" and "Sen".
 */
@Composable
fun PairedFlow(
    me: UserProfile,
    coupleId: String,
    onNotificationAnswer: (allowed: Boolean) -> Unit,
    onLocationAnswer: (allowed: Boolean) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val app = lovelyyApp()
    // Decided once when the flow starts, so the "1 / 2" counter doesn't change under the user's finger.
    val needNotifications = rememberSaveable {
        Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }
    val needLocation = rememberSaveable { !CoarseLocation.hasPermission(context) }
    val primers = listOf(needNotifications, needLocation).count { it }
    var step by rememberSaveable { mutableIntStateOf(STEP_PAIRED) }

    fun after(current: Int) {
        when {
            current < STEP_NOTIFICATIONS && needNotifications -> step = STEP_NOTIFICATIONS
            current < STEP_LOCATION && needLocation -> step = STEP_LOCATION
            else -> onDone()
        }
    }

    val distanceVm: DistanceViewModel = viewModel(key = "onboarding-distance-$coupleId") { DistanceViewModel(app, coupleId) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onNotificationAnswer(granted)
        after(STEP_NOTIFICATIONS)
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Same sharing as the Mesafe screen's first "Konumumu paylaş": city and distance only.
        if (granted) distanceVm.share(context, me.uid, cityOnly = true, first = true) { }
        onLocationAnswer(granted)
        after(STEP_LOCATION)
    }

    BackHandler(enabled = step != STEP_PAIRED) {
        step = if (step == STEP_LOCATION && needNotifications) STEP_NOTIFICATIONS else STEP_PAIRED
    }

    AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "paired-flow") { s ->
        when (s) {
            STEP_PAIRED -> PairedStep(onNext = { after(STEP_PAIRED) })
            STEP_NOTIFICATIONS -> NotificationStep(
                index = 1, total = primers,
                onAllow = { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) },
                onLater = {
                    onNotificationAnswer(false)
                    after(STEP_NOTIFICATIONS)
                },
            )
            else -> LocationStep(
                index = primers, total = primers,
                onBack = if (needNotifications) ({ step = STEP_NOTIFICATIONS }) else null,
                onAllow = { askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                onLater = {
                    onLocationAnswer(false)
                    after(STEP_LOCATION)
                },
            )
        }
    }
}

@Composable
private fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
private fun StepText(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
        Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp, color = Brown)
        Spacer(Modifier.height(10.dp))
        Text(body, fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = BrownSoft)
    }
}

@Composable
private fun StepButtons(main: String, onMain: () -> Unit, later: String?, onLater: () -> Unit, leading: ImageVector? = null) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = if (later == null) 20.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val lead: (@Composable RowScope.() -> Unit)? =
            if (leading != null) ({ Icon(leading, null, tint = Brown, modifier = Modifier.size(22.dp)) }) else null
        PeachButton(main, onMain, leading = lead)
        if (later != null) {
            Spacer(Modifier.height(6.dp))
            GhostButton(later, onLater)
        }
    }
}

@Composable
private fun PersonAvatar(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .softShadow(CircleShape, 8.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (size > 70.dp) 5.dp else 4.dp, CreamCard, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Person, null, tint = Brown, modifier = Modifier.size(size * 0.46f))
    }
}

/** "İzinler · 1 / 2" with an optional back button. */
@Composable
private fun PrimerTopBar(index: Int, total: Int, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 14.dp).height(44.dp)) {
        if (onBack != null) BackButton(onBack, Modifier.align(Alignment.CenterStart))
        Pill(
            if (total > 1) stringResource(R.string.onb_permissions_step, index, total) else stringResource(R.string.onb_permissions),
            Modifier.align(Alignment.Center),
        )
    }
}

// ---------- A4: paired ----------

@Composable
private fun PairedStep(onNext: () -> Unit) {
    val top = statusBarTop()
    val enter = remember { Animatable(0f) }
    var catShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(700)
        catShown = true
    }
    val beat by rememberInfiniteTransition(label = "beat").animateFloat(
        1f, 1.14f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s",
    )

    Column(Modifier.fillMaxSize().background(Cream)) {
        SplitStage(Modifier.weight(1f)) {
            // The two sides meet on the seam, with the cat between them.
            Box(Modifier.fillMaxSize().padding(top = top), contentAlignment = Alignment.Center) {
                PersonAvatar(
                    Mint, 92.dp,
                    Modifier.offset(x = (-65).dp, y = (-6).dp).graphicsLayer { translationY = -(1f - enter.value) * 60.dp.toPx() },
                )
                PersonAvatar(
                    Peach, 92.dp,
                    Modifier.offset(x = 65.dp, y = 26.dp).graphicsLayer { translationY = (1f - enter.value) * 60.dp.toPx() },
                )
                PopIn(catShown, Modifier.offset(y = 10.dp)) {
                    Box(
                        Modifier
                            .size(90.dp)
                            .softShadow(CircleShape, 12.dp)
                            .clip(CircleShape)
                            .background(CatBg)
                            .border(5.dp, CreamCard, CircleShape)
                    ) {
                        Image(
                            painterResource(R.drawable.cat_base),
                            contentDescription = stringResource(R.string.onb_paired_cat_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Box(
                    Modifier
                        .offset(y = 82.dp)
                        .graphicsLayer { scaleX = beat; scaleY = beat }
                        .size(40.dp)
                        .softShadow(CircleShape, 4.dp)
                        .clip(CircleShape)
                        .background(CreamCard),
                    contentAlignment = Alignment.Center,
                ) { SvgIcon(HEART_D, HeartInk, Modifier.size(20.dp)) }
            }
        }
        StepText(stringResource(R.string.onb_paired_title), stringResource(R.string.onb_paired_body))
        SoftCard(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp), radius = 22.dp, padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.onb_paired_cat_label),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp, color = BrownSoft,
                )
                Text(stringResource(R.string.onb_paired_cat_name), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brown)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MintTint).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Edit, null, tint = MintInk, modifier = Modifier.padding(top = 1.dp).size(18.dp))
                Text(
                    stringResource(R.string.onb_paired_cat_note),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = MintInk,
                )
            }
        }
        StepButtons(stringResource(R.string.onb_next), onNext, later = null, onLater = {})
    }
}

// ---------- A5: notifications ----------

@Composable
private fun NotificationStep(index: Int, total: Int, onAllow: () -> Unit, onLater: () -> Unit) {
    val top = statusBarTop()
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(200)
        repeat(3) {
            shown++
            delay(500)
        }
    }

    Column(Modifier.fillMaxSize().background(Cream)) {
        SplitStage(Modifier.weight(1f)) {
            PrimerTopBar(index, total, onBack = null, Modifier.align(Alignment.TopCenter))
            SideChip(stringResource(R.string.onb_notif_sender), Mint, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = top + 76.dp)) {
                Icon(Icons.Rounded.Person, null, tint = Brown, modifier = Modifier.size(18.dp))
            }
            Row(Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = top + 72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SenderIcon(Peach) { SvgIcon(HEART_D, Brown, Modifier.size(20.dp)) }
                SenderIcon(CreamCard) { Icon(Icons.Rounded.TouchApp, null, tint = Brown, modifier = Modifier.size(20.dp)) }
                SenderIcon(PeachSoft) { Icon(Icons.Rounded.SentimentSatisfied, null, tint = Brown, modifier = Modifier.size(20.dp)) }
            }
            // What they send lands on your side, one by one.
            Column(
                Modifier.align(Alignment.Center).offset(y = 36.dp).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Dropping(shown >= 1) {
                    StageBubble(stringResource(R.string.onb_notif_card_love), stringResource(R.string.onb_notif_card_now), Modifier.fillMaxWidth()) {
                        Badge(Peach, 44.dp) { SvgIcon(HEART_D, Brown, Modifier.size(22.dp)) }
                    }
                }
                Dropping(shown >= 2) {
                    StageBubble(stringResource(R.string.onb_notif_card_cat), stringResource(R.string.onb_notif_card_cat_sub), Modifier.fillMaxWidth()) {
                        Badge(CatBg, 44.dp) {
                            Image(painterResource(R.drawable.cat_base), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
                Dropping(shown >= 3) {
                    StageBubble(stringResource(R.string.onb_notif_card_day), stringResource(R.string.onb_notif_card_day_sub), Modifier.fillMaxWidth()) {
                        Badge(MintSoft, 44.dp) { Icon(Icons.Rounded.CalendarMonth, null, tint = Brown, modifier = Modifier.size(22.dp)) }
                    }
                }
            }
        }
        StepText(stringResource(R.string.onb_notif_title), stringResource(R.string.onb_notif_body))
        StepButtons(stringResource(R.string.onb_notif_allow), onAllow, stringResource(R.string.onb_later), onLater, Icons.Rounded.NotificationsNone)
    }
}

@Composable
private fun SenderIcon(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).softShadow(CircleShape, 4.dp).clip(CircleShape).background(color).border(3.dp, CreamCard, CircleShape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Dropping(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } + fadeIn(tween(200)),
    ) { content() }
}

// ---------- A6: location ----------

@Composable
private fun LocationStep(index: Int, total: Int, onBack: (() -> Unit)?, onAllow: () -> Unit, onLater: () -> Unit) {
    val top = statusBarTop()
    val phase by rememberInfiniteTransition(label = "dash").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p",
    )

    Column(Modifier.fillMaxSize().background(Cream)) {
        SplitStage(Modifier.weight(1f)) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                // The line between you: from the partner's avatar (top right) to yours (bottom left).
                Canvas(Modifier.fillMaxSize()) {
                    val from = Offset(size.width - 84.dp.toPx(), (top + 102.dp).toPx())
                    val to = Offset(84.dp.toPx(), size.height - 68.dp.toPx())
                    val line = Path().apply {
                        moveTo(from.x, from.y)
                        cubicTo(size.width * 0.5f, from.y + 16.dp.toPx(), to.x + 50.dp.toPx(), size.height * 0.55f, to.x, to.y)
                    }
                    val gap = 12.dp.toPx()
                    drawPath(
                        line, Brown,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), gap), phase * (2.dp.toPx() + gap))),
                    )
                }
                Row(
                    Modifier.align(Alignment.TopEnd).padding(end = 54.dp, top = top + 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Pill(stringResource(R.string.home_partner_fallback), fontSize = 14)
                    PersonAvatar(Mint, 60.dp)
                }
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 54.dp, bottom = 38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PersonAvatar(Peach, 60.dp)
                    Pill(stringResource(R.string.onb_draw_me), fontSize = 14)
                }
                Text(
                    stringResource(R.string.onb_loc_unknown_km),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Brown, maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = seamAngle(wPx, hPx) }
                        .softShadow(RoundedCornerShape(18.dp), 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CreamCard)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
            PrimerTopBar(index, total, onBack, Modifier.align(Alignment.TopCenter))
        }
        StepText(stringResource(R.string.onb_loc_title), stringResource(R.string.onb_loc_body))
        SoftCard(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp), radius = 20.dp, padding = 14.dp) {
            PromiseRow(Icons.Rounded.Lock, stringResource(R.string.onb_loc_promise_who))
            Spacer(Modifier.height(10.dp))
            PromiseRow(Icons.Rounded.LocationOn, stringResource(R.string.onb_loc_promise_what))
        }
        StepButtons(stringResource(R.string.onb_loc_allow), onAllow, stringResource(R.string.onb_later), onLater)
    }
}

@Composable
private fun PromiseRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MintInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Brown)
    }
}
