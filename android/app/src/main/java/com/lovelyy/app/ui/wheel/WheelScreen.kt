package com.lovelyy.app.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.data.Wheel
import com.lovelyy.app.data.WheelMath
import com.lovelyy.app.data.WheelRepository
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.CLOCK_D
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SheetField
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

private val WedgeColors = listOf(Peach, PeachTint, Mint, Kit.Butter)
private val SpinEasing = CubicBezierEasing(0.15f, 0.8f, 0.2f, 1f)
private const val WHEEL_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z M12 3.5v17M3.5 12h17M6 6l12 12M18 6L6 18"
private const val POINTER_D = "M17 28L4 5a3 3 0 0 1 2.6-4.5h20.8A3 3 0 0 1 30 5z"
private const val VISIBLE_CHIPS = 4

class WheelViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val stored: StateFlow<List<Wheel>?> = app.wheelRepository.observe(coupleId)
        .catch { Timber.w(it, "Wheels unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(id: String?, label: String, items: List<String>) {
        viewModelScope.launch {
            runCatching { app.wheelRepository.save(coupleId, id, label, items) }
                .onSuccess { if (id == null) Telemetry.track(AnalyticsEvent.WheelCreated) }
                .onFailure { Timber.w(it, "Saving the wheel failed") }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { runCatching { app.wheelRepository.delete(coupleId, id) }.onFailure { Timber.w(it, "Deleting the wheel failed") } }
    }
}

/** Karar çarkı (approved design 13): pick a wheel, spin, edit its options. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WheelScreen(me: UserProfile, onBack: () -> Unit, onHistory: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: WheelViewModel = viewModel(key = "wheel-$coupleId") { WheelViewModel(app, coupleId) }
    val stored by vm.stored.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Built-in wheels (edited copies override them), then custom ones.
    val defaults = listOf(
        Wheel("yemek", stringResource(R.string.wh_yemek), stringArrayResource(R.array.wh_items_yemek).toList(), builtIn = true),
        Wheel("gezi", stringResource(R.string.wh_gezi), stringArrayResource(R.array.wh_items_gezi).toList(), builtIn = true),
        Wheel("izle", stringResource(R.string.wh_izle), stringArrayResource(R.array.wh_items_izle).toList(), builtIn = true),
    )
    val saved = stored.orEmpty().associateBy { it.id }
    val wheels = defaults.map { d -> saved[d.id]?.let { s -> d.copy(items = s.items.ifEmpty { d.items }) } ?: d } +
        stored.orEmpty().filter { !it.builtIn }

    var selectedId by rememberSaveable { mutableStateOf("yemek") }
    val wheel = wheels.firstOrNull { it.id == selectedId } ?: wheels.first()
    val items = wheel.items

    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    // No result until this wheel has been spun once.
    var spun by remember(selectedId) { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var selectNewest by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(stored) {
        val newest = stored.orEmpty().filter { !it.builtIn }.maxByOrNull { it.at }
        if (selectNewest && newest != null) { selectedId = newest.id; selectNewest = false }
    }

    fun spin() {
        if (spinning || items.size < WheelMath.MIN_ITEMS) return
        spinning = true
        val target = Random.nextInt(items.size)
        val to = WheelMath.spinTo(rotation.value, target, items.size, turns = 4, jitter = Random.nextFloat() * 2f - 1f)
        Telemetry.track(AnalyticsEvent.WheelSpun(wheel.builtIn, items.size))
        scope.launch {
            rotation.animateTo(to, tween(3200, easing = SpinEasing))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            spinning = false
            spun = true
        }
    }

    fun saveItems(newItems: List<String>) = vm.save(wheel.id, wheel.label, newItems)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(stringResource(R.string.wh_title), onBack) {
            RoundSvgButton(CLOCK_D, stringResource(R.string.wh_history), onHistory)
        }

        // Wheel picker + "new wheel".
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                wheels.forEach { w ->
                    val sel = w.id == wheel.id
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (sel) Brown else CreamCard)
                            .semantics { role = Role.Tab }
                            .bounceClick(enabled = !spinning) { selectedId = w.id; showAll = false }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(w.label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (sel) Cream else Brown, maxLines = 1, softWrap = false)
                    }
                }
            }
            DashedCircleButton(stringResource(R.string.wh_new)) { creating = true }
        }

        // The wheel with its pointer.
        Box(Modifier.fillMaxWidth().padding(top = 0.dp), contentAlignment = Alignment.TopCenter) {
            val pointer = remember { svgPath(POINTER_D) }
            Canvas(Modifier.size(34.dp, 30.dp).zIndex(2f)) {
                scale(size.width / 34f, pivot = Offset.Zero) { drawPath(pointer, Brown) }
            }
            Box(
                Modifier.padding(top = 14.dp).size(280.dp).softShadow(CircleShape, 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                WheelFace(items, rotation.value, wheel.label, Modifier.size(280.dp))
                Box(
                    Modifier.size(60.dp).clip(CircleShape).background(Cream).border(4.dp, Brown, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { SvgIcon(HEART_D, Color(0xFFE07A45), Modifier.size(24.dp)) }
            }
        }

        // Result.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (spinning) R.string.wh_spinning else if (spun) R.string.wh_result else R.string.wh_not_spun),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft,
            )
            val result = if (spinning || items.isEmpty()) "…" else if (!spun) "?" else items[WheelMath.indexAt(rotation.value, items.size)]
            Text(result, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        MainAction(stringResource(R.string.wh_spin), WHEEL_D, ::spin, enabled = !spinning && items.size >= WheelMath.MIN_ITEMS)

        // Options.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.wh_list, wheel.label, items.size), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val shown = if (showAll) items else items.take(VISIBLE_CHIPS)
                shown.forEach { item ->
                    OptionChip(item, removeLabel = stringResource(R.string.wh_remove, item), canRemove = items.size > WheelMath.MIN_ITEMS && !spinning) {
                        saveItems(items - item)
                    }
                }
                if (items.size > VISIBLE_CHIPS) {
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kit.ButtonIdle)
                            .bounceClick { showAll = !showAll }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (showAll) stringResource(R.string.wh_less) else stringResource(R.string.wh_more, items.size - VISIBLE_CHIPS),
                            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
                        )
                    }
                }
                if (items.size < WheelMath.MAX_ITEMS) DashedPill(stringResource(R.string.wh_add)) { adding = true }
            }
            // Edited built-in → back to default; custom → delete.
            if (wheel.builtIn && saved.containsKey(wheel.id)) {
                LinkText(stringResource(R.string.wh_reset)) { vm.delete(wheel.id) }
            } else if (!wheel.builtIn) {
                LinkText(stringResource(R.string.wh_delete_wheel), Color(0xFFA8401C)) { vm.delete(wheel.id); selectedId = "yemek" }
            }
        }
    }

    if (adding) {
        AddOptionSheet(onDismiss = { adding = false }) { text ->
            adding = false
            if (text.isNotBlank() && items.none { it.equals(text.trim(), ignoreCase = true) }) saveItems(items + text.trim())
        }
    }
    if (creating) {
        NewWheelSheet(onDismiss = { creating = false }) { label, list ->
            creating = false
            selectNewest = true
            vm.save(null, label, list)
        }
    }
}

/** Wedges from the top, clockwise; labels read from the center outwards like the design. */
@Composable
private fun WheelFace(items: List<String>, rotation: Float, label: String, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = if (items.size > 8) 11.sp else 13.sp, color = Brown)
    Canvas(modifier.semantics { contentDescription = label }) {
        val s = size.width / 280f
        val c = Offset(size.width / 2, size.height / 2)
        val r = 132f * s
        val n = items.size.coerceAtLeast(1)
        val wedge = 360f / n
        rotate(rotation, c) {
            val topLeft = Offset(c.x - r, c.y - r)
            val arcSize = Size(r * 2, r * 2)
            for (i in 0 until n) {
                drawArc(WedgeColors[i % WedgeColors.size], -90f + i * wedge, wedge, useCenter = true, topLeft = topLeft, size = arcSize)
                drawArc(Cream, -90f + i * wedge, wedge, useCenter = true, topLeft = topLeft, size = arcSize, style = Stroke(3f * s))
            }
            items.forEachIndexed { i, text ->
                val mid = -90f + (i + 0.5f) * wedge
                rotate(mid, c) {
                    val layout = measurer.measure(
                        text, style, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        constraints = Constraints(maxWidth = (88f * s).toInt()),
                    )
                    // SVG: text-anchor middle at x=224, baseline at y=145 (center is 140,140).
                    val x = c.x + 84f * s - layout.size.width / 2f
                    val y = c.y + 5f * s - layout.firstBaseline
                    // Labels on the left half would read upside down: turn them around their own center.
                    val screen = (((mid + rotation) % 360f) + 360f) % 360f
                    val flip = screen > 90f && screen < 270f
                    rotate(if (flip) 180f else 0f, Offset(c.x + 84f * s, c.y)) {
                        drawText(layout, topLeft = Offset(x, y))
                    }
                }
            }
        }
        drawCircle(Brown, radius = r, center = c, style = Stroke(4f * s))
    }
}

@Composable
private fun OptionChip(text: String, removeLabel: String, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        Modifier.height(44.dp).softShadow(RoundedCornerShape(50), 2.dp).clip(RoundedCornerShape(50)).background(CreamCard)
            .padding(start = 14.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
        Box(
            Modifier.size(40.dp).semantics { role = Role.Button; contentDescription = removeLabel }
                .bounceClick(enabled = canRemove, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { SvgStrokeIcon(Kit.X_D, BrownSoft.copy(alpha = if (canRemove) 1f else 0.4f), Modifier.size(14.dp), strokeWidth = 2.4f) }
    }
}

@Composable
private fun DashedPill(text: String, onClick: () -> Unit) {
    Box(Modifier.height(44.dp).bounceClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = 2.dp.toPx()
            drawRoundRect(
                Kit.Dashed, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SvgStrokeIcon(Kit.PLUS_D, Brown, Modifier.size(16.dp), strokeWidth = 2.4f)
            Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
        }
    }
}

@Composable
private fun DashedCircleButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).semantics { role = Role.Button; contentDescription = label }.bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = 2.dp.toPx()
            drawCircle(Kit.Dashed, radius = size.minDimension / 2 - w / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
        }
        SvgStrokeIcon(Kit.PLUS_D, Brown, Modifier.size(18.dp), strokeWidth = 2.4f)
    }
}

@Composable
private fun LinkText(text: String, color: Color = BrownSoft, onClick: () -> Unit) {
    Text(
        text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color,
        modifier = Modifier.bounceClick(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOptionSheet(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.wh_add_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
            SheetField(text, { text = it.take(WheelRepository.MAX_ITEM) }, stringResource(R.string.wh_add_hint))
            MainAction(stringResource(R.string.wh_add), Kit.PLUS_D, { onAdd(text) }, enabled = text.isNotBlank())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewWheelSheet(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf("") }
    var lines by rememberSaveable { mutableStateOf("") }
    val list = lines.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.wh_new_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
            SheetField(name, { name = it.take(WheelRepository.MAX_LABEL) }, stringResource(R.string.wh_new_name))
            SheetField(lines, { lines = it.take(400) }, stringResource(R.string.wh_new_items), singleLine = false, minHeight = 140.dp)
            val problem = when {
                list.size < WheelMath.MIN_ITEMS -> stringResource(R.string.wh_min_items, WheelMath.MIN_ITEMS)
                list.size > WheelMath.MAX_ITEMS -> stringResource(R.string.wh_max_items, WheelMath.MAX_ITEMS)
                else -> null
            }
            if (problem != null && lines.isNotBlank()) {
                Text(problem, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
            }
            MainAction(stringResource(R.string.wh_save), Kit.CHECK_D, { onCreate(name.trim(), list) }, enabled = name.isNotBlank() && problem == null)
        }
    }
}
