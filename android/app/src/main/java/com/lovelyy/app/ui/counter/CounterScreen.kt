package com.lovelyy.app.ui.counter

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Milestone
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.LovelyyDatePicker
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.NeedOk
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

private val TR = Locale.forLanguageTag("tr-TR")
private val MintCell = Color(0xFFD6F0E8)
private val ButterCell = Color(0xFFFFF1C9)
private val CellLabel = Color(0xFF5F524C)
private val MsColor = Color(0xFFB5532A)
private val HeartOrange = Color(0xFFE07A45)
private val PathTrack = Color(0xFFF3EAE3)
private val DashGrey = Color(0xFFB8A69D)

private const val PENCIL_D = "M4 20l4-1 11-11-3-3L5 16z"
private const val SHARE_D = "M14 5h3a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-3M10 8l-4 4 4 4M6 12h9"
private const val CHECK_D = "M5 12.5l4.5 4.5L19 7.5"

class CounterViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    /** The cat's name, for "O gün Mırmır parti şapkasını takacak". */
    val catName: StateFlow<String?> = app.catRepository.observe(coupleId)
        .map { it.name?.takeIf(String::isNotBlank) }
        .catch { Timber.w(it, "Cat name unavailable on the counter") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setStart(date: LocalDate) {
        viewModelScope.launch {
            runCatching { app.userRepository.setStartDate(coupleId, DayCounter.startMillis(date)) }
                .onSuccess { Telemetry.track(AnalyticsEvent.StartDateSet) }
                .onFailure { Timber.w(it, "Saving the start date failed") }
        }
    }

    fun shared() = Telemetry.track(AnalyticsEvent.CounterShared)
}

/** Gün sayacı (approved design 5): progress ring, live ticker, totals and the milestone path. */
@Composable
fun CounterScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val coupleId = couple?.id ?: me.coupleId ?: return
    val vm: CounterViewModel = viewModel(key = "counter-$coupleId") { CounterViewModel(app, coupleId) }
    val catName by vm.catName.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val startMillis = couple?.startDate?.toDate()?.time
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var picking by rememberSaveable { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startMillis) { while (startMillis != null) withFrameMillis { now = System.currentTimeMillis() } }
    val t = DayCounter.together(startMillis ?: now, now)
    val days = t.days
    val startDate = startMillis?.let { DayCounter.dateOf(it) }
    val (prev, next) = DayCounter.next(days, startDate)
    val all = DayCounter.milestones(days + 2000, startDate)
    val todayMilestone = startMillis != null && all.any { it.day == days }

    val shareText = stringResource(R.string.dc_share_text, me.displayName, partnerName, num(days))

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CenteredTopBar(stringResource(R.string.feature_days), onBack) {
                RoundSvgButton(PENCIL_D, stringResource(R.string.dc_edit_date), { picking = true })
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Ring(
                    progress = (days.toFloat() / next.day.toFloat()).coerceIn(0f, 1f),
                    days = days,
                    me = me.displayName,
                    partner = partnerName,
                )
                Ticker(t.hours, t.minutes, t.seconds, t.millis)
                Text(
                    if (startMillis != null) stringResource(R.string.dc_since, sinceText(startMillis)) else stringResource(R.string.dc_no_date),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
                )
            }

            val months = startMillis?.let { Period.between(DayCounter.dateOf(it), DayCounter.dateOf(now)).toTotalMonths() } ?: 0L
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell(num(days / 7), stringResource(R.string.dc_weeks), PeachSoft, Modifier.weight(1f))
                StatCell(num(months), stringResource(R.string.dc_months), MintCell, Modifier.weight(1f))
                StatCell(num(t.totalMillis / 3_600_000L), stringResource(R.string.dc_hours), ButterCell, Modifier.weight(1f))
            }

            MilestonePath(days, prev, next, all, startMillis, catName ?: stringResource(R.string.dc_cat_fallback))

            MainButton(
                text = stringResource(if (startMillis != null) R.string.dc_share else R.string.dc_pick),
                icon = if (startMillis != null) SHARE_D else PENCIL_D,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (startMillis == null) {
                    picking = true
                } else {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                    runCatching { context.startActivity(Intent.createChooser(send, null)) }.onSuccess { vm.shared() }
                }
            }
        }

        if (todayMilestone) Confetti(Modifier.fillMaxSize())
    }

    if (picking) {
        LovelyyDatePicker(
            initial = startMillis?.let { DayCounter.dateOf(it) },
            title = stringResource(R.string.counter_picker_title),
            allowFuture = false,
            onDismiss = { picking = false },
            onPick = { date ->
                picking = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.setStart(date)
            },
        )
    }
}

private fun num(n: Long): String = NumberFormat.getIntegerInstance(TR).format(n)

/** "9 Ekim 2025 Perşembe'den" */
private fun sinceText(startMillis: Long): String {
    val d = DayCounter.dateOf(startMillis)
    return d.format(DateTimeFormatter.ofPattern("d MMMM yyyy ", TR)) +
        TurkishSuffix.ablative(d.format(DateTimeFormatter.ofPattern("EEEE", TR)))
}

private fun longDate(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM yyyy EEEE", TR))

@Composable
private fun beatScale(big: Float = 1.12f, small: Float = 1.08f): Float {
    val s by rememberInfiniteTransition(label = "beat").animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; big at 270; 1f at 540; small at 810; 1f at 1080 }),
        label = "s",
    )
    return s
}

@Composable
private fun Ring(progress: Float, days: Long, me: String, partner: String) {
    val animated by animateFloatAsState(progress, tween(900), label = "ring")
    Box(Modifier.size(width = 236.dp, height = 250.dp)) {
        Canvas(Modifier.size(236.dp)) {
            val stroke = 16.dp.toPx()
            val r = 104.dp.toPx()
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(CreamCard, r, c)
            drawCircle(PeachSoft, r, c, style = Stroke(stroke))
            drawArc(
                NeedLow, -90f, 360f * animated, false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(Modifier.size(236.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.dc_together), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrownSoft)
            Text(
                num(days),
                style = TextStyle(fontFeatureSettings = "tnum"),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = if (days >= 10_000) 64.sp else 80.sp, lineHeight = 80.sp, color = Brown,
            )
            Text(stringResource(R.string.dc_day_word), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
        }
        Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
            Initial(me, Peach)
            Box(
                Modifier.zIndex(1f).size(30.dp).scale(beatScale()).clip(CircleShape).background(Cream),
                contentAlignment = Alignment.Center,
            ) { SvgIcon(HEART_D, HeartOrange, Modifier.size(18.dp)) }
            Initial(partner, Mint)
        }
    }
}

@Composable
private fun Initial(name: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase(TR) ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Brown)
    }
}

@Composable
private fun Ticker(h: Int, m: Int, s: Int, ms: Int) {
    Row(
        Modifier
            .softShadow(RoundedCornerShape(50), 6.dp)
            .clip(RoundedCornerShape(50))
            .background(CreamCard)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TickPart("%02d".format(h), stringResource(R.string.dc_unit_h))
        TickPart("%02d".format(m), stringResource(R.string.dc_unit_m))
        TickPart("%02d".format(s), stringResource(R.string.dc_unit_s))
        TickPart("%03d".format(ms), stringResource(R.string.dc_unit_ms), MsColor)
    }
}

@Composable
private fun TickPart(value: String, unit: String, color: Color = Brown) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = TextStyle(fontFeatureSettings = "tnum"), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(unit, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun StatCell(value: String, label: String, bg: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = TextStyle(fontFeatureSettings = "tnum"), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, maxLines = 1)
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CellLabel)
    }
}

@Composable
private fun nodeLabel(m: Milestone): String = if (m.years != null) stringResource(R.string.dc_node_year, m.years) else m.day.toString()

@Composable
private fun MilestonePath(days: Long, prev: Milestone?, next: Milestone, all: List<Milestone>, startMillis: Long?, catName: String) {
    // Six stops: up to four reached ones, the next one, and what comes after.
    val done = all.filter { it.day <= days }.takeLast(4)
    val nodes = done + all.filter { it.day > days }.take(6 - done.size)
    val from = prev?.day ?: 0L
    val frac = ((days - from).toFloat() / (next.day - from).toFloat()).coerceIn(0f, 1f)
    val left = next.day - days

    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(CreamCard)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dc_path_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, modifier = Modifier.weight(1f))
            Text(
                if (next.years != null) stringResource(R.string.dc_until_year, next.years, left) else stringResource(R.string.dc_until_day, next.day, left),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(PeachSoft).padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val n = nodes.size
            val col = maxWidth / n
            // Track from the first stop's center to the last one's; mint fill up to where we are now.
            val fillTo: Dp = if (done.isEmpty()) col / 2 * frac else col * (done.size - 1 + frac)
            Box(Modifier.offset(x = col / 2, y = 14.dp).width(col * (n - 1)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(PathTrack))
            Box(Modifier.offset(x = col / 2, y = 14.dp).width(fillTo).height(4.dp).clip(RoundedCornerShape(2.dp)).background(NeedOk))
            Row(Modifier.fillMaxWidth()) {
                nodes.forEachIndexed { i, m ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when {
                            m.day <= days -> Box(
                                Modifier.size(32.dp).clip(CircleShape).background(Mint).border(3.dp, CreamCard, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { SvgStrokeIcon(CHECK_D, Brown, Modifier.size(14.dp), strokeWidth = 3f) }
                            i == done.size -> Box(
                                Modifier.size(32.dp).scale(beatScale()).clip(CircleShape).background(Peach).border(3.dp, Brown, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { SvgIcon(HEART_D, Brown, Modifier.size(14.dp)) }
                            else -> Canvas(Modifier.size(32.dp)) {
                                drawCircle(CreamCard)
                                val w = 2.dp.toPx()
                                drawCircle(
                                    DashGrey, size.minDimension / 2 - w / 2,
                                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))),
                                )
                            }
                        }
                        Text(
                            nodeLabel(m), fontFamily = Quicksand,
                            fontWeight = if (i > done.size) FontWeight.SemiBold else FontWeight.Bold,
                            fontSize = 12.sp, color = Brown, maxLines = 1,
                        )
                    }
                }
            }
        }

        if (startMillis != null) {
            Text(
                stringResource(R.string.dc_next_line, longDate(DayCounter.dateOf(startMillis).plusDays(next.day)), catName),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun MainButton(text: String, icon: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .softShadow(shape, 12.dp, Peach)
            .clip(shape)
            .background(Peach)
            .semantics { role = Role.Button }
            .bounceClick(pressedScale = 0.97f, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, textAlign = TextAlign.Center, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Celebration day: a short burst of confetti when the screen opens. */
@Composable
private fun Confetti(modifier: Modifier) {
    val colors = listOf(Peach, Mint, Color(0xFFFFD9A0), Color(0xFFF8B7C8), NeedLow)
    val pieces = remember {
        List(70) {
            floatArrayOf(Random.nextFloat(), Random.nextFloat() * 0.4f, 0.6f + Random.nextFloat() * 0.8f, Random.nextFloat() * 360f, it.toFloat())
        }
    }
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) { time.animateTo(1f, tween(3200, easing = LinearEasing)) }
    if (time.value >= 1f) return
    Canvas(modifier) {
        val p = time.value
        pieces.forEach { (x, delay, speed, spin, idx) ->
            val local = ((p - delay * 0.5f) / 0.8f).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -20f + local * speed * size.height
            val cx = x * size.width + kotlin.math.sin((local * 6f + x * 10f).toDouble()).toFloat() * 24f
            rotate(spin + local * 540f, Offset(cx, y)) {
                drawRect(colors[idx.toInt() % colors.size], Offset(cx - 6f, y - 3f), Size(12f, 6f), alpha = 1f - local * 0.6f)
            }
        }
    }
}
