package com.lovelyy.app.ui.drawing

import com.lovelyy.app.text.AppLocale
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.SavedDrawing
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.ConfirmDialog
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.history.DAY_MONTH
import com.lovelyy.app.ui.history.DAY_MONTH_YEAR
import com.lovelyy.app.ui.history.EmptyNote
import com.lovelyy.app.ui.history.InnerColumn
import com.lovelyy.app.ui.history.MiniAvatar
import com.lovelyy.app.ui.history.Muted
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.history.clock
import com.lovelyy.app.ui.history.localDate
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter

private const val TRASH_D = "M5 7h14M10 7V5h4v2M7 7l1 12a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2l1-12M10 11v6M14 11v6"
private const val LEFT_D = "M15 6l-6 6 6 6"
private const val RIGHT_D = "M9 6l6 6-6 6"
private val LONG_DAY: DateTimeFormatter get() = AppLocale.formatter("dMMMMyEEEE")

/**
 * Çizimlerimiz (approved boards 28-29): every drawing saved from Canlı çizim in a two-column grid, and one drawing
 * up close with newer/older paging and delete. Deleting removes it for both partners.
 */
@Composable
fun DrawingsScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onNew: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val drawings by remember(coupleId) { app.drawingRepository.observeSaved(coupleId).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(null)
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val list = drawings.orEmpty()
    val open = list.indexOfFirst { it.id == openId }.takeIf { it >= 0 }

    BackHandler(enabled = openId != null) { openId = null }
    // The open drawing was deleted (here or on the partner's phone): back to the grid.
    LaunchedEffect(open, drawings) { if (openId != null && drawings != null && open == null) openId = null }

    if (open != null) {
        DrawingDetail(me, partnerName, list, open, onBack = { openId = null }, onMove = { openId = list[it].id })
        return
    }

    InnerColumn(spacing = 12) {
        CenteredTopBar(stringResource(R.string.dg_title), onBack)
        // P9: every saved drawing stays here; only saving more needs Plus.
        com.lovelyy.app.ui.plus.PlusLimitBanner(com.lovelyy.app.ui.plus.rememberLimitGate(com.lovelyy.app.data.PlusLimits.Limit.Drawings, list.size))
        if (drawings != null) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Brown)) { append(pluralStringResource(R.plurals.dg_count, list.size.toInt(), list.size)) }
                    append(" · ")
                    append(stringResource(R.string.dg_sub))
                },
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubText, modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (drawings != null && list.isEmpty()) EmptyNote(stringResource(R.string.dg_empty))
        list.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d -> DrawingTile(d, me, partnerName, Modifier.weight(1f)) { openId = d.id } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        MainAction(stringResource(R.string.dg_new), Kit.PENCIL_D, onNew, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DrawingTile(d: SavedDrawing, me: UserProfile, partnerName: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val date = d.at.localDate()
    val label = stringResource(R.string.dg_tile, date.format(DAY_MONTH))
    Column(
        modifier.softShadow(shape, 6.dp).clip(shape).background(Color.White).semantics { contentDescription = label }
            .bounceClick(pressedScale = 0.97f, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Cream).padding(6.dp)) {
            DrawingPreview(d.strokes, Modifier.fillMaxSize())
        }
        Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(date.format(DAY_MONTH), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, modifier = Modifier.weight(1f))
            Artists(d, me, partnerName, 20)
        }
    }
}

/** Overlapping initials of whoever drew on it (me peach, partner mint). */
@Composable
private fun Artists(d: SavedDrawing, me: UserProfile, partnerName: String, size: Int) {
    val uids = d.strokes.map { it.by }.toSet().ifEmpty { setOf(d.by) }
    Row {
        val mine = me.uid in uids
        if (mine) MiniAvatar(me.displayName, Peach, size)
        if (uids.any { it != me.uid }) MiniAvatar(partnerName, Mint, size, if (mine) Modifier.offset(x = (-size / 3).dp) else Modifier)
    }
}

@Composable
private fun DrawingDetail(me: UserProfile, partnerName: String, list: List<SavedDrawing>, index: Int, onBack: () -> Unit, onMove: (Int) -> Unit) {
    val d = list[index]
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.dg_delete_failed)
    val uids = d.strokes.map { it.by }.toSet().ifEmpty { setOf(d.by) }
    val who = when {
        uids.size > 1 -> stringResource(R.string.dg_together)
        me.uid in uids -> stringResource(R.string.dg_by_me)
        else -> stringResource(R.string.dg_by, partnerName)
    }

    InnerColumn(spacing = 12) {
        CenteredTopBar(d.at.localDate().format(DAY_MONTH_YEAR), onBack) {
            RoundSvgButton(TRASH_D, stringResource(R.string.dg_delete), { confirm = true })
        }
        val shape = RoundedCornerShape(28.dp)
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).softShadow(shape, 10.dp).clip(shape).background(Color.White).padding(18.dp)
                .semantics { contentDescription = context.getString(R.string.lv_canvas) },
        ) {
            DrawingPreview(d.strokes, Modifier.fillMaxSize())
        }
        WhiteCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Artists(d, me, partnerName, 32)
                Column(Modifier.weight(1f)) {
                    Text(who, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                    Text(
                        d.at.localDate().format(LONG_DAY) + " · " + d.at.clock(),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PageLink(stringResource(R.string.dg_newer), LEFT_D, leading = true, enabled = index > 0) { onMove(index - 1) }
            Text(
                "${index + 1} / ${list.size}", Modifier.weight(1f), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            PageLink(stringResource(R.string.dg_older), RIGHT_D, leading = false, enabled = index < list.lastIndex) { onMove(index + 1) }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.dg_delete_title),
            body = stringResource(R.string.dg_delete_body),
            confirm = stringResource(R.string.dg_delete_yes),
            onConfirm = {
                confirm = false
                val coupleId = me.coupleId ?: return@ConfirmDialog
                scope.launch {
                    try {
                        app.drawingRepository.deleteSaved(coupleId, d.id)
                        com.lovelyy.app.telemetry.Telemetry.track(com.lovelyy.app.analytics.AnalyticsEvent.DrawingDeleted)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Deleting a saved drawing failed")
                        Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun PageLink(text: String, icon: String, leading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Brown else Muted.copy(alpha = 0.5f)
    Row(
        Modifier.heightIn(min = 44.dp).then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading) SvgStrokeIcon(icon, color, Modifier.size(18.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        if (!leading) SvgStrokeIcon(icon, color, Modifier.size(18.dp))
    }
}
