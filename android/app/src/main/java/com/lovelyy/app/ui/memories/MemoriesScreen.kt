package com.lovelyy.app.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.data.Capsule
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Dream
import com.lovelyy.app.data.DrawingMath
import com.lovelyy.app.data.Note
import com.lovelyy.app.data.SavedDrawing
import com.lovelyy.app.data.Stroke
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.TabHeader
import com.lovelyy.app.ui.components.TagChip
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.drawing.DrawingPreview
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val NOTE_D = "M6 4h12a2 2 0 0 1 2 2v8l-6 6H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M14 20v-6h6"

class MemoriesViewModel(app: LovelyyApplication, coupleId: String) : ViewModel() {
    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        catch { Timber.w(it, "Memories source unavailable") }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val notes = app.noteRepository.observe(coupleId).state(emptyList())
    val capsules = app.capsuleRepository.observe(coupleId).state(emptyList())
    val dreams = app.dreamRepository.observe(coupleId).state(emptyList())
    val saved = app.drawingRepository.observeSaved(coupleId).state(emptyList())
    val strokes = app.drawingRepository.observe(coupleId).state(emptyList())
    val presence = app.drawingRepository.observePresence(coupleId).state(emptyMap())
}

/** One line of "Son anılar". [who] null = something you did together. */
private data class Memory(val text: String, val at: Long, val who: String?, val mine: Boolean)

/** Anılar tab (approved design 16): live drawing, notes, time capsule and recent memories. */
@Composable
fun MemoriesScreen(
    me: UserProfile,
    couple: Couple?,
    partner: UserProfile?,
    onDrawing: () -> Unit,
    onNotes: () -> Unit,
    onCapsule: () -> Unit,
    onSearch: () -> Unit,
) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: MemoriesViewModel = viewModel(key = "memories-$coupleId") { MemoriesViewModel(app, coupleId) }
    val notes by vm.notes.collectAsStateWithLifecycle()
    val capsules by vm.capsules.collectAsStateWithLifecycle()
    val dreams by vm.dreams.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val strokes by vm.strokes.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = com.lovelyy.app.ui.coupleToday()
    val partnerLive = partner?.uid?.let { presence[it] }?.let { System.currentTimeMillis() - it < DrawingMath.ACTIVE_MS } == true

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TabHeader(me.displayName, partnerName, stringResource(R.string.mm_title), stringResource(R.string.mm_subtitle)) {
            RoundSvgButton(Kit.SEARCH_D, stringResource(R.string.mm_search), onSearch)
        }

        // Live drawing.
        WhiteCard(padding = 14.dp, radius = 28.dp, onClick = onDrawing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(132.dp).clip(RoundedCornerShape(20.dp)).background(Cream).padding(8.dp)) {
                    val preview: List<Stroke> = strokes.ifEmpty { saved.firstOrNull()?.strokes.orEmpty() }
                    DrawingPreview(preview, Modifier.fillMaxSize())
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.lv_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                    Text(stringResource(R.string.mm_drawing_sub), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
                    if (partnerLive) TagChip(stringResource(R.string.lv_partner_now, partnerName), Kit.MintCell)
                }
            }
        }

        // Notes + time capsule.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val latest = notes.maxByOrNull { it.at }
            SmallTile(
                NOTE_D, PeachSoft, stringResource(R.string.feature_notes),
                if (notes.isEmpty()) stringResource(R.string.mm_notes_none) else stringResource(R.string.mm_notes_count, notes.size),
                latest?.let { stringResource(R.string.mm_notes_new, it.text.lineSequence().first().take(18).let { t -> if (it.text.length > 18) "$t…" else t }) },
                Modifier.weight(1f), onNotes,
            )
            val sealed = capsules.filter { !it.isOpen(today) }
            val nearest = sealed.minByOrNull { it.openOn }
            SmallTile(
                Kit.ENVELOPE_D, Kit.MintCell, stringResource(R.string.tc_title),
                if (sealed.isEmpty()) stringResource(R.string.mm_capsule_none) else stringResource(R.string.mm_capsule_count, sealed.size),
                nearest?.let { stringResource(R.string.mm_capsule_next, ChronoUnit.DAYS.between(today, it.openOn).toInt()) }
                    ?: stringResource(R.string.mm_capsule_write),
                Modifier.weight(1f), onCapsule,
            )
        }

        // Recent memories.
        val memories = recentMemories(me, partnerName, couple, notes, capsules, dreams, saved, today)
        WhiteCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.mm_recent), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                if (memories.isEmpty()) {
                    Text(stringResource(R.string.mm_recent_none), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
                }
                memories.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (m.who != null) InitialAvatar(m.who, if (m.mine) Peach else Mint, 22.dp, 11.sp, border = false)
                        else Box(Modifier.size(22.dp).clip(CircleShape).background(Kit.Butter))
                        Text(m.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Brown, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dayLabel(m.at, today), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallTile(icon: String, tint: Color, title: String, line1: String, line2: String?, modifier: Modifier, onClick: () -> Unit) {
    WhiteCard(modifier, padding = 14.dp, radius = 22.dp, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                SvgStrokeIcon(icon, Brown, Modifier.size(22.dp))
            }
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            Text(line1, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, maxLines = 1)
            Text(line2.orEmpty(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 16.dp))
        }
    }
}

@Composable
private fun recentMemories(
    me: UserProfile,
    partnerName: String,
    couple: Couple?,
    notes: List<Note>,
    capsules: List<Capsule>,
    dreams: List<Dream>,
    saved: List<SavedDrawing>,
    today: LocalDate,
): List<Memory> {
    fun who(uid: String, name: String) = if (uid == me.uid) me.displayName else name.ifBlank { partnerName }
    val list = mutableListOf<Memory>()
    notes.take(5).forEach { list += Memory(stringResource(R.string.mm_ev_note, who(it.by, it.byName)), it.at, who(it.by, it.byName), it.by == me.uid) }
    saved.take(5).forEach { list += Memory(stringResource(R.string.mm_ev_drawing), it.at, who(it.by, it.byName), it.by == me.uid) }
    dreams.take(10).forEach { d ->
        list += Memory(stringResource(R.string.mm_ev_dream_added, who(d.by, d.byName)), d.at, who(d.by, d.byName), d.by == me.uid)
        d.doneAt?.let { list += Memory(stringResource(R.string.mm_ev_dream_done, d.title), it, null, false) }
    }
    capsules.take(5).forEach { list += Memory(stringResource(R.string.mm_ev_capsule, who(it.by, it.byName)), it.at, who(it.by, it.byName), it.by == me.uid) }
    // Milestones from the day counter: the 100th day and every year.
    couple?.startDate?.toDate()?.time?.let { start ->
        val first = DayCounter.dateOf(start)
        val hundred = first.plusDays(99)
        if (!hundred.isAfter(today)) list += Memory(stringResource(R.string.mm_ev_100), hundred.toEpochMillis(), null, false)
        var year = 1
        while (!first.plusYears(year.toLong()).isAfter(today)) {
            list += Memory(stringResource(R.string.mm_ev_year, year), first.plusYears(year.toLong()).toEpochMillis(), null, false)
            year++
        }
    }
    return list.sortedByDescending { it.at }.take(5)
}

private fun LocalDate.toEpochMillis(): Long = atStartOfDay(com.lovelyy.app.data.CoupleClock.zone()).toInstant().toEpochMilli() + 12 * 3_600_000L

@Composable
private fun dayLabel(at: Long, today: LocalDate): String {
    val d = DayCounter.dateOf(at)
    return when (d) {
        today -> stringResource(R.string.mm_today)
        today.minusDays(1) -> stringResource(R.string.mm_yesterday)
        else -> d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMM" else "d MMM yyyy", Locale.forLanguageTag("tr-TR")))
    }
}
