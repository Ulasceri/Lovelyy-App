package com.lovelyy.app.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.DrawingMath
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.cat.CatViewModel
import com.lovelyy.app.ui.cat.bubbleRes
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.drawing.DrawingPreview
import com.lovelyy.app.ui.drawing.DrawingViewModel
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.widget.CatWideWidgetProvider
import com.lovelyy.app.widget.CatWidgetProvider
import com.lovelyy.app.widget.DaysWidgetProvider
import com.lovelyy.app.widget.DrawingWidgetProvider
import java.time.LocalDate

private val Wallpaper = Color(0xFFEAD9CB)
private val CellLabel = Color(0xFF5F524C)
private val CatBg = Color(0xFFFAE2C0)
private val BarTrack = Color(0xFFF3EAE3)
private val BarLow = Color(0xFFF07B45)
private val BarOk = Color(0xFF5DB89E)
private val LiveCell = Color(0xFFD6F0E8)
private val LiveGreen = Color(0xFF2F7F69)
private val Muted = Color(0xFF75655E)
private val WidgetShape = RoundedCornerShape(28.dp)

/**
 * "Ana ekran widget'ları" (design 15): the four widgets as they look on the home screen, with the couple's
 * real data. Tapping one asks the launcher to add it.
 */
@Composable
fun WidgetsScreen(me: UserProfile, couple: Couple?, partner: UserProfile?) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val cat: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val catUi by cat.ui.collectAsStateWithLifecycle()
    val drawing: DrawingViewModel = viewModel(key = "drawing-$coupleId") { DrawingViewModel(app, coupleId, me.uid) }
    val strokes by drawing.strokes.collectAsStateWithLifecycle()
    val presence by drawing.presence.collectAsStateWithLifecycle()

    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val catName = catUi?.doc?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cat_generic)
    val levels = catUi?.levels
    val streak = catUi?.doc?.streak?.current(LocalDate.now()) ?: 0
    val days = couple?.startDate?.toDate()?.time?.let { DayCounter.together(it, System.currentTimeMillis()).days.toInt() }
    val liveAt = partner?.uid?.let { presence[it] }
    val partnerLive = liveAt != null && System.currentTimeMillis() - liveAt < DrawingMath.ACTIVE_MS
    val failText = stringResource(R.string.wg_pin_failed)
    fun pin(provider: Class<out AppWidgetProvider>) {
        if (!requestPin(context, provider)) Toast.makeText(context, failText, Toast.LENGTH_SHORT).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Wallpaper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 28.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.pf_widgets), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
            Text(stringResource(R.string.wg_subtitle), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CellLabel)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // Kedi · küçük
            Labeled(stringResource(R.string.wg_cat_small), Modifier.weight(1f)) {
                WidgetBox(Modifier.height(168.dp), bg = CatBg, onClick = { pin(CatWidgetProvider::class.java) }) {
                    CatPicture(levels?.sleeping == true, Modifier.fillMaxSize())
                    Text(
                        catName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Text(
                        stringResource(levels?.mood?.bubbleRes() ?: R.string.bubble_idle),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            // Canlı çizim
            Labeled(stringResource(R.string.wg_drawing), Modifier.weight(1f)) {
                WidgetBox(Modifier.height(168.dp), bg = Color.White, onClick = { pin(DrawingWidgetProvider::class.java) }) {
                    val s = strokes.orEmpty()
                    if (s.isEmpty()) {
                        Text(
                            stringResource(R.string.widget_drawing_empty), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        )
                    } else {
                        DrawingPreview(s, Modifier.fillMaxSize().padding(12.dp))
                    }
                    if (partnerLive) {
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(10.dp).clip(CircleShape).background(LiveCell)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(LiveGreen))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.lv_partner_now, partnerName), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown)
                        }
                    }
                }
            }
        }

        // Kedi · geniş
        Labeled(stringResource(R.string.wg_cat_wide)) {
            WidgetBox(Modifier.height(168.dp), onClick = { pin(CatWideWidgetProvider::class.java) }) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(150.dp).fillMaxHeight().background(CatBg)) { CatPicture(levels?.sleeping == true, Modifier.fillMaxSize()) }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(catName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.widget_streak, streak), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
                                modifier = Modifier.clip(CircleShape).background(PeachSoft).padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Bar(stringResource(R.string.need_hunger), levels?.hunger)
                        Bar(stringResource(R.string.need_thirst), levels?.thirst)
                        Bar(stringResource(R.string.need_energy), levels?.energy)
                        Bar(stringResource(R.string.need_love), levels?.love)
                        Text(
                            if (days != null) stringResource(R.string.widget_couple_day, stringResource(R.string.home_us, me.displayName, partnerName), days)
                            else stringResource(R.string.home_us, me.displayName, partnerName),
                            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Gün sayacı + tek dokunuşla kalp
        Labeled(stringResource(R.string.wg_days)) {
            WidgetBox(Modifier.height(84.dp), onClick = { pin(DaysWidgetProvider::class.java) }) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(74.dp)) {
                        InitialAvatar(me.displayName, Peach, 44.dp, 17.sp)
                        InitialAvatar(partnerName, Mint, 44.dp, 17.sp, Modifier.padding(start = 30.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.widget_together), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Muted)
                        Text(
                            if (days != null) stringResource(R.string.widget_days, days) else stringResource(R.string.home_set_date),
                            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = if (days != null) 22.sp else 15.sp, color = Brown,
                        )
                    }
                    val beat by rememberInfiniteTransition(label = "wgBeat").animateFloat(
                        1f, 1f,
                        infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; 1.12f at 270; 1f at 540; 1.08f at 810; 1f at 1080 }),
                        label = "s",
                    )
                    Box(Modifier.size(48.dp).scale(beat).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                        SvgIcon(HEART_D, Brown, Modifier.size(22.dp))
                    }
                }
            }
        }

        // The design's faded app icons: "this is the home screen".
        repeat(2) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = if (it == 0) 6.dp else 0.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(4) { Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.55f))) }
            }
        }
        Text(
            stringResource(R.string.wg_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CellLabel,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun Labeled(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun WidgetBox(modifier: Modifier, bg: Color = Cream, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.fillMaxWidth().bounceClick(pressedScale = 0.97f, onClick = onClick)
            .softShadow(WidgetShape, 14.dp).clip(WidgetShape).background(bg),
        content = content,
    )
}

@Composable
private fun CatPicture(sleeping: Boolean, modifier: Modifier) {
    Image(
        painterResource(if (sleeping) R.drawable.cat_sleep else R.drawable.cat_base), null,
        contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, 0.1f), modifier = modifier,
    )
}

@Composable
private fun Bar(label: String, level: Double?) {
    val v = ((level ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, maxLines = 1, modifier = Modifier.width(44.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(3.dp)).background(BarTrack)) {
            Box(Modifier.fillMaxWidth(v).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (v < 0.4f) BarLow else BarOk))
        }
    }
}

/** Asks the launcher to add this widget. False if the launcher can't. */
private fun requestPin(context: Context, provider: Class<out AppWidgetProvider>): Boolean {
    val m = AppWidgetManager.getInstance(context)
    if (!m.isRequestPinAppWidgetSupported) return false
    return runCatching { m.requestPinAppWidget(ComponentName(context, provider), null, null) }.getOrDefault(false)
}
