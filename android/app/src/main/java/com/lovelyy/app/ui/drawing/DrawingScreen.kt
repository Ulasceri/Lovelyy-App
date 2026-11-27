package com.lovelyy.app.ui.drawing

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.DrawingMath
import com.lovelyy.app.data.Stroke as LineStroke
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.SwitchRow
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.heartbeat.timeAgo
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.widget.DrawingWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Pen colors from the design. */
val PenColors = listOf(0xFFF07B45, 0xFF3E9C82, 0xFF4A3B35, 0xFFE86A8A, 0xFFF2B632)
private const val ERASER = 0xFFFFFFFF
private val BrushWidths = listOf(0.012f, 0.022f, 0.04f)
private val LiveGreen = Color(0xFF2F7F69)

private const val BRUSH_D = "M14 4l6 6-8 8H6v-6z M4 20c2 0 3-1 3-2.5"
private const val ERASER_D = "M4 16l9-10 7 6-7 8H8z M9 11l6 6M4 20h16"
private const val UNDO_D = "M8 8L4 12l4 4M4 12h10a5 5 0 0 1 0 10h-2"

class DrawingViewModel(private val app: LovelyyApplication, private val coupleId: String, private val myUid: String) : ViewModel() {
    val strokes: StateFlow<List<LineStroke>?> = app.drawingRepository.observe(coupleId)
        .catch { Timber.w(it, "Drawing unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val presence: StateFlow<Map<String, Long>> = app.drawingRepository.observePresence(coupleId)
        .catch { Timber.w(it, "Presence unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** How many drawings are saved to Anılar (for the free limit, PlusLimits.Limit.Drawings). */
    // A count aggregation (one read), not a listener on every saved drawing (big documents) just to count them.
    private val _savedCount = MutableStateFlow(0)
    val savedCount: StateFlow<Int> = _savedCount

    init { refreshSavedCount() }

    private fun refreshSavedCount() {
        viewModelScope.launch { app.drawingRepository.savedCount(coupleId)?.let { _savedCount.value = it.toInt() } }
    }

    private var lastTouch = 0L

    fun add(color: Long, width: Float, points: List<Float>) {
        viewModelScope.launch {
            runCatching { app.drawingRepository.add(coupleId, myUid, color, width, points) }
                .onSuccess { touch() }
                .onFailure { Timber.w(it, "Saving the stroke failed") }
        }
    }

    /** "I'm drawing": at most every 20 s. */
    fun touch() {
        val now = System.currentTimeMillis()
        if (now - lastTouch < 20_000) return
        lastTouch = now
        viewModelScope.launch { runCatching { app.drawingRepository.touch(coupleId, myUid) } }
    }

    fun undo() {
        val mine = strokes.value.orEmpty().lastOrNull { it.by == myUid } ?: return
        viewModelScope.launch { runCatching { app.drawingRepository.delete(coupleId, mine.id) }.onFailure { Timber.w(it, "Undo failed") } }
    }

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    fun save(me: UserProfile, onDone: () -> Unit) {
        val current = strokes.value.orEmpty()
        if (current.isEmpty() || !_saving.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                // false = the partner saved this canvas first; it is cleared already, so just carry on quietly.
                val saved = app.drawingRepository.saveAndClear(coupleId, me, current)
                if (saved) Telemetry.track(AnalyticsEvent.DrawingSaved(current.size))
                refreshSavedCount()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Saving the drawing failed")
            } finally {
                _saving.value = false
            }
        }
    }
}

/** Canlı çizim (approved design 17): one canvas, both of you draw on it. */
@Composable
fun DrawingScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onOld: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val vm: DrawingViewModel = viewModel(key = "drawing-$coupleId") { DrawingViewModel(app, coupleId, me.uid) }
    val strokes by vm.strokes.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val savedCount by vm.savedCount.collectAsStateWithLifecycle()
    // P9: a free couple saves up to 10 drawings. No Plus screen ever opens while drawing, so a blocked save only
    // explains itself; the gallery (Çizimlerimiz) carries the P9 note and the way to Plus.
    val saveGate = com.lovelyy.app.ui.plus.rememberLimitGate(com.lovelyy.app.data.PlusLimits.Limit.Drawings, savedCount)
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)

    var colorIndex by remember { mutableIntStateOf(0) }
    var widthIndex by remember { mutableIntStateOf(1) }
    var eraser by remember { mutableStateOf(false) }
    var widgetOn by remember { mutableStateOf(DrawingWidget.isEnabled(context)) }

    // "Now" ticks so the live chip turns off by itself.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(10_000); now = System.currentTimeMillis() } }
    val partnerAt = partner?.uid?.let { presence[it] }
    val partnerLive = partnerAt != null && now - partnerAt < DrawingMath.ACTIVE_MS

    // Keep the home-screen widget in step with the canvas.
    LaunchedEffect(strokes, widgetOn, partnerLive) {
        strokes?.let {
            val stamp = DrawingMath.stampOf(it).takeIf { _ -> it.size < com.lovelyy.app.data.DrawingRepository.MAX_STROKES }
            DrawingWidget.save(context, it, if (partnerLive) partnerName else null, stamp)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(stringResource(R.string.lv_title), onBack) {
            RoundSvgButton(Kit.PHOTO_D, stringResource(R.string.lv_old), onOld)
        }

        // Who is here.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(if (partnerLive) Kit.MintCell else Kit.ButtonIdle)
                    .padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InitialAvatar(partnerName, Mint, 22.dp, 11.sp, border = false)
                Text(
                    when {
                        partnerLive -> stringResource(R.string.lv_partner_now, partnerName)
                        partnerAt != null -> stringResource(R.string.lv_partner_last, partnerName, timeAgo(partnerAt))
                        else -> stringResource(R.string.lv_partner_never, partnerName)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                )
                if (partnerLive) {
                    val beat by rememberInfiniteTransition(label = "live").animateFloat(
                        1f, 1f,
                        infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; 1.3f at 270; 1f at 540; 1.2f at 810; 1f at 1080 }),
                        label = "b",
                    )
                    Box(Modifier.size(8.dp).scale(beat).clip(CircleShape).background(LiveGreen))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InitialAvatar(me.displayName, Peach, 22.dp, 11.sp, border = false)
                Text(stringResource(R.string.lv_you), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
            }
        }

        // The canvas.
        val list = strokes.orEmpty()
        val color = if (eraser) ERASER else PenColors[colorIndex]
        val width = BrushWidths[widthIndex] * if (eraser) 2f else 1f
        DrawingCanvas(
            strokes = list,
            color = color,
            width = width,
            hint = if (list.isEmpty()) stringResource(R.string.lv_empty_hint, partnerName) else null,
            partnerLabel = if (partnerLive) partnerName else null,
            partnerUid = partner?.uid,
            onStart = vm::touch,
            onStroke = { pts -> vm.add(color, width, pts) },
            modifier = Modifier.fillMaxWidth().height(356.dp),
        )

        // Tools.
        WhiteCard(padding = 0.dp, radius = 28.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenColors.forEachIndexed { i, c ->
                    val sel = !eraser && i == colorIndex
                    val label = stringResource(R.string.lv_color, i + 1)
                    Box(
                        Modifier.size(44.dp).semantics { role = Role.RadioButton; selected = sel; contentDescription = label }
                            .bounceClick { colorIndex = i; eraser = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(30.dp)
                                .then(if (sel) Modifier.border(2.dp, Brown, CircleShape).padding(5.dp) else Modifier)
                                .clip(CircleShape).background(Color(c)),
                        )
                    }
                }
                // The pen cycles thin → medium → thick; the dot on it shows the current size and color.
                Box {
                    ToolButton(BRUSH_D, stringResource(R.string.lv_brush_size, widthIndex + 1), false) { widthIndex = (widthIndex + 1) % BrushWidths.size }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(listOf(4.dp, 7.dp, 11.dp)[widthIndex]).clip(CircleShape).background(Color(PenColors[colorIndex])))
                    }
                }
                ToolButton(ERASER_D, stringResource(R.string.lv_eraser), eraser) { eraser = !eraser }
                ToolButton(UNDO_D, stringResource(R.string.lv_undo), false, vm::undo)
            }
        }

        WhiteCard(padding = 0.dp) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                SwitchRow(stringResource(R.string.lv_widget), stringResource(R.string.lv_widget_sub), widgetOn) { on ->
                    widgetOn = on
                    DrawingWidget.setEnabled(context, on)
                }
            }
        }

        val savedText = stringResource(R.string.lv_saved)
        val limitText = pluralStringResource(
            R.plurals.limit_drawings_toast, com.lovelyy.app.data.PlusLimits.Limit.Drawings.free, com.lovelyy.app.data.PlusLimits.Limit.Drawings.free,
        )
        MainAction(stringResource(R.string.lv_save), Kit.PHOTO_D, {
            eraser = false
            if (saveGate.blocked) Toast.makeText(context, limitText, Toast.LENGTH_LONG).show()
            else vm.save(me) { Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show() }
        }, enabled = list.isNotEmpty() && !saving)
    }
}

@Composable
private fun ToolButton(d: String, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (active) Peach else Kit.ButtonIdle)
            .semantics { role = Role.Button; contentDescription = label; selected = active }
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { SvgStrokeIcon(d, Brown, Modifier.size(20.dp)) }
}

/** The white canvas: saved strokes, my stroke while I draw, and a little name tag where the partner drew last. */
@Composable
private fun DrawingCanvas(
    strokes: List<LineStroke>,
    color: Long,
    width: Float,
    hint: String?,
    partnerLabel: String?,
    partnerUid: String?,
    onStart: () -> Unit,
    onStroke: (List<Float>) -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val current = remember { mutableStateListOf<Float>() }
    val start by rememberUpdatedState(onStart)
    val finish by rememberUpdatedState(onStroke)
    val canvasLabel = stringResource(R.string.lv_canvas)
    BoxWithConstraints(modifier.softShadow(shape, 12.dp).clip(shape).background(CreamCard)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // Finished strokes: paths built once per canvas change, on their own layer, so the stroke being drawn
        // (redrawn on every finger move) doesn't rebuild up to 600 paths each frame.
        val finished = remember(strokes, w, h) {
            strokes.map { PreparedStroke(normalizedPath(it.points, w, h), Color(it.color), roundStroke(it.width * w)) }
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            finished.forEach { drawPath(it.path, it.color, style = it.style) }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = canvasLabel }
                .pointerInput(w, h) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        start()
                        current.clear()
                        current += (down.position.x / w).coerceIn(0f, 1f); current += (down.position.y / h).coerceIn(0f, 1f)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            current += (change.position.x / w).coerceIn(0f, 1f); current += (change.position.y / h).coerceIn(0f, 1f)
                        }
                        // A tap becomes a dot.
                        if (current.size == 2) { current += current[0] + 0.001f; current += current[1] }
                        finish(current.toList())
                        current.clear()
                    }
                },
        ) {
            if (current.size >= 2) drawNormalized(current, Color(color), width)
        }
        if (hint != null) {
            Text(
                hint, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
        // Partner's name next to their latest stroke while they draw.
        val last = partnerUid?.let { uid -> strokes.lastOrNull { it.by == uid } }
        if (partnerLabel != null && last != null && last.points.size >= 2) {
            val density = LocalDensity.current
            val x = last.points[last.points.size - 2] * w
            val y = last.points[last.points.size - 1] * h
            val bob by rememberInfiniteTransition(label = "tag").animateFloat(
                0f, -4f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
            )
            Text(
                partnerLabel,
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White,
                modifier = Modifier
                    .offset(x = with(density) { (x + 6f).coerceAtMost(w - 80f).toDp() }, y = with(density) { (y - 30f).coerceAtLeast(0f).toDp() })
                    .graphicsLayer { translationY = bob.dp.toPx() }
                    .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp))
                    .background(LiveGreen)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
    }
}

/** A finished stroke ready to paint: its path in pixels, color and stroke style. */
private class PreparedStroke(val path: Path, val color: Color, val style: Stroke)

private fun roundStroke(px: Float) = Stroke(px, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** A normalized (0..1) point list as a path on a [w] x [h] canvas. */
private fun normalizedPath(points: List<Float>, w: Float, h: Float): Path {
    val path = Path()
    if (points.size < 2) return path
    path.moveTo(points[0] * w, points[1] * h)
    var i = 2
    while (i + 1 < points.size) {
        path.lineTo(points[i] * w, points[i + 1] * h)
        i += 2
    }
    return path
}

/** Draws a normalized stroke into this canvas (the stroke being drawn right now). */
fun DrawScope.drawNormalized(points: List<Float>, color: Color, width: Float, blendMode: BlendMode = BlendMode.SrcOver) {
    if (points.size < 2) return
    drawPath(normalizedPath(points, size.width, size.height), color, style = roundStroke(width * size.width), blendMode = blendMode)
}

/** A small read-only preview of a drawing (Anılar card). */
@Composable
fun DrawingPreview(strokes: List<LineStroke>, modifier: Modifier) {
    // Eraser strokes are white on the white canvas; here the card behind is tinted, so clear them instead.
    // Paths are built once per size and drawing (drawWithCache), not on every redraw of the card.
    androidx.compose.foundation.layout.Spacer(
        modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
            val prepared = strokes.filter { it.points.size >= 2 }.map {
                PreparedStroke(normalizedPath(it.points, size.width, size.height), Color(it.color), roundStroke(it.width * size.width)) to
                    (if (it.color == ERASER) BlendMode.Clear else BlendMode.SrcOver)
            }
            onDrawBehind { prepared.forEach { (s, blend) -> drawPath(s.path, s.color, style = s.style, blendMode = blend) } }
        },
    )
}
