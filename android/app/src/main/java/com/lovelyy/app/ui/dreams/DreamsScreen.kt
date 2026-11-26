package com.lovelyy.app.ui.dreams

import com.lovelyy.app.text.AppLocale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Dream
import com.lovelyy.app.data.DreamCategory
import com.lovelyy.app.data.DreamConflictException
import com.lovelyy.app.data.DreamRepository
import com.lovelyy.app.data.ItemGoneException
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.components.ChoicePill
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.CoupleAvatars
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SheetField
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class DreamsViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val dreams: StateFlow<List<Dream>?> = app.dreamRepository.observe(coupleId)
        .catch { Timber.w(it, "Dreams unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * [opened]: the dream as it was when the editor opened (null for a new one). [onGone]: the partner deleted
     * it meanwhile (it is not re-created). [onConflict]: the partner changed it meanwhile; gets the server
     * version. [force] overwrites.
     */
    fun save(
        me: UserProfile,
        opened: Dream?,
        title: String,
        category: DreamCategory,
        goal: String,
        force: Boolean = false,
        onGone: () -> Unit,
        onConflict: (Dream) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { app.dreamRepository.save(coupleId, me, opened, title, category, goal, force) }
                .onSuccess { if (opened == null) Telemetry.track(AnalyticsEvent.DreamAdded(category.key)) }
                .onFailure {
                    when (it) {
                        is DreamConflictException -> onConflict(it.current)
                        is ItemGoneException -> { Timber.w(it, "Saving the dream failed"); onGone() }
                        else -> Timber.w(it, "Saving the dream failed")
                    }
                }
        }
    }

    fun setDone(id: String, done: Boolean, onGone: () -> Unit) {
        viewModelScope.launch {
            runCatching { app.dreamRepository.setDone(coupleId, id, done) }
                .onSuccess { if (done) Telemetry.track(AnalyticsEvent.DreamCompleted) }
                .onFailure { Timber.w(it, "Updating the dream failed"); if (it is ItemGoneException) onGone() }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { runCatching { app.dreamRepository.delete(coupleId, id) }.onFailure { Timber.w(it, "Deleting the dream failed") } }
    }
}

private class Editing(val dream: Dream?)

/** An edit that hit a newer change by the partner: [current] is the server version, the rest is my unsaved edit. */
private class DreamConflict(val current: Dream, val title: String, val category: DreamCategory, val goal: String)

/** Hayaller listesi (approved design 12): the shared bucket list. */
@Composable
fun DreamsScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onSearch: () -> Unit, openId: String? = null) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: DreamsViewModel = viewModel(key = "dreams-$coupleId") { DreamsViewModel(app, coupleId) }
    val dreams by vm.dreams.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf<Editing?>(null) }
    // Opened from Arama on one dream: open it once the list has loaded.
    var opened by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(dreams, openId) {
        if (!opened && openId != null) dreams?.firstOrNull { it.id == openId }?.let { editing = Editing(it); opened = true }
    }

    // "Emin misin?" before deleting.
    var askDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    askDelete?.let { act ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.del_dream_title),
            body = stringResource(R.string.del_dream_body),
            confirm = stringResource(R.string.del_confirm),
            onConfirm = { act(); askDelete = null },
            onDismiss = { askDelete = null },
        )
    }

    // The partner deleted the dream that is open here: close the sheet and say so.
    val context = androidx.compose.ui.platform.LocalContext.current
    val goneText = stringResource(R.string.gone_dream)
    val showGone = { android.widget.Toast.makeText(context, goneText, android.widget.Toast.LENGTH_SHORT).show() }
    val editingId = editing?.dream?.id
    androidx.compose.runtime.LaunchedEffect(dreams, editingId) {
        val list = dreams
        if (editingId != null && list != null && list.none { it.id == editingId }) { editing = null; showGone() }
    }

    val all = dreams.orEmpty()
    val open = all.filter { it.doneAt == null }.sortedByDescending { it.at }
    val done = all.filter { it.doneAt != null }.sortedByDescending { it.doneAt }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(stringResource(R.string.dr_title), onBack) {
            RoundSvgButton(Kit.SEARCH_D, stringResource(R.string.dr_search), onSearch)
        }

        // Progress: how many came true.
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 12.dp).clip(shape).background(PeachSoft).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.dr_done_together), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Kit.CellLabel)
                    Text(stringResource(R.string.dr_count, done.size, all.size), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Brown)
                }
                CoupleAvatars(me.displayName, partnerName, size = 44.dp, fontSize = 17.sp)
            }
            val progress by animateFloatAsState(if (all.isEmpty()) 0f else done.size.toFloat() / all.size, label = "dreams")
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.8f))) {
                Box(Modifier.fillMaxWidth(progress).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kit.Green))
            }
        }

        DreamList(stringResource(R.string.dr_next), open, stringResource(if (done.isEmpty()) R.string.dr_empty else R.string.dr_empty_next), me, partnerName,
            onToggle = { d -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.setDone(d.id, true, showGone) },
            onOpen = { editing = Editing(it) })
        if (done.isNotEmpty() || all.isNotEmpty()) {
            DreamList(stringResource(R.string.dr_done), done, stringResource(R.string.dr_none_done), me, partnerName,
                onToggle = { d -> vm.setDone(d.id, false, showGone) },
                onOpen = { editing = Editing(it) })
        }

        MainAction(stringResource(R.string.dr_add), Kit.PLUS_D, { editing = Editing(null) })
    }

    // The partner changed the dream while it was open here: ask before overwriting their version.
    var conflict by remember { mutableStateOf<DreamConflict?>(null) }
    conflict?.let { c ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.dr_conflict_title),
            body = stringResource(R.string.dr_conflict_body, partnerName),
            confirm = stringResource(R.string.n_conflict_overwrite),
            onConfirm = {
                conflict = null
                vm.save(me, c.current, c.title, c.category, c.goal, force = true, onGone = showGone)
            },
            // Keep the partner version: reopen the editor with it.
            onDismiss = { conflict = null; editing = Editing(c.current) },
        )
    }

    editing?.let { e ->
        // key(): a reopened editor (after a conflict) starts from the new version, not the old draft.
        androidx.compose.runtime.key(e.dream?.id, e.dream?.updatedAt, e.dream?.title, e.dream?.goal, e.dream?.category) {
            DreamEditor(
                dream = e.dream,
                onDismiss = { editing = null },
                onSave = { title, cat, goal ->
                    vm.save(
                        me, e.dream, title, cat, goal,
                        onGone = showGone,
                        onConflict = { current -> conflict = DreamConflict(current, title, cat, goal) },
                    )
                    editing = null
                },
                onDelete = e.dream?.let { d -> { askDelete = { vm.delete(d.id); editing = null } } },
            )
        }
    }
}

@Composable
private fun DreamList(
    title: String,
    items: List<Dream>,
    empty: String,
    me: UserProfile,
    partnerName: String,
    onToggle: (Dream) -> Unit,
    onOpen: (Dream) -> Unit,
) {
    WhiteCard(Modifier, padding = 0.dp) {
        Column(Modifier.padding(start = 8.dp, end = 14.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.padding(start = 10.dp))
            if (items.isEmpty()) {
                Text(empty, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft, modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 6.dp))
            }
            items.forEach { d -> DreamRow(d, me, partnerName, onToggle, onOpen) }
        }
    }
}

@Composable
private fun DreamRow(d: Dream, me: UserProfile, partnerName: String, onToggle: (Dream) -> Unit, onOpen: (Dream) -> Unit) {
    val done = d.doneAt != null
    val mine = d.by == me.uid
    val toggleLabel = stringResource(if (done) R.string.dr_mark_undone else R.string.dr_mark_done, d.title)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .semantics { role = Role.Checkbox; contentDescription = toggleLabel }
                .bounceClick { onToggle(d) },
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                    SvgStrokeIcon(Kit.CHECK_D, Brown, Modifier.size(16.dp), strokeWidth = 2.8f)
                }
            } else {
                Canvas(Modifier.size(30.dp)) {
                    val w = 2.5.dp.toPx()
                    drawCircle(Kit.Dashed, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
                }
            }
        }
        Column(Modifier.weight(1f).bounceClick(pressedScale = 0.98f) { onOpen(d) }) {
            Text(
                d.title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (done) BrownSoft else Brown, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle(d), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        InitialAvatar(if (mine) me.displayName else d.byName.ifBlank { partnerName }, if (mine) Peach else Mint, 22.dp, 11.sp, border = false)
    }
}

@Composable
private fun subtitle(d: Dream): String {
    val done = d.doneAt
    if (done != null) return DayCounter.dateOf(done).format(AppLocale.formatter("dMMMMy"))
    val cat = categoryLabel(d.category)
    return if (d.goal.isBlank()) cat else "$cat · ${d.goal}"
}

@Composable
internal fun categoryLabel(c: DreamCategory): String = stringResource(
    when (c) {
        DreamCategory.Seyahat -> R.string.dr_cat_seyahat
        DreamCategory.Deneyim -> R.string.dr_cat_deneyim
        DreamCategory.Hayat -> R.string.dr_cat_hayat
        DreamCategory.Diger -> R.string.dr_cat_diger
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DreamEditor(dream: Dream?, onDismiss: () -> Unit, onSave: (String, DreamCategory, String) -> Unit, onDelete: (() -> Unit)?) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(dream?.title.orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(dream?.goal.orEmpty()) }
    var cat by rememberSaveable { mutableStateOf(dream?.category ?: DreamCategory.Seyahat) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (dream == null) R.string.dr_editor_new else R.string.dr_editor_edit),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            )
            SheetField(title, { title = it.take(DreamRepository.MAX_TITLE) }, stringResource(R.string.dr_editor_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DreamCategory.entries.forEach { c -> ChoicePill(categoryLabel(c), c == cat, { cat = c }) }
            }
            SheetField(goal, { goal = it.take(DreamRepository.MAX_GOAL) }, stringResource(R.string.dr_editor_goal))
            MainAction(stringResource(R.string.dr_save), Kit.CHECK_D, { onSave(title, cat, goal) }, enabled = title.isNotBlank())
            if (onDelete != null) {
                Text(
                    stringResource(R.string.dr_delete),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFA8401C),
                    modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick(onClick = onDelete).padding(12.dp),
                )
            }
        }
    }
}
