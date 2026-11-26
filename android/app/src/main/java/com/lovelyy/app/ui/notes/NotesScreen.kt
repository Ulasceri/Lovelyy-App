package com.lovelyy.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.lovelyy.app.data.ItemGoneException
import com.lovelyy.app.data.Note
import com.lovelyy.app.data.NoteRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val FootInk = Color(0xFF5F524C)
private val FieldBorder = Color(0xFFF0E2D8)

/** Sticky-note colors from the design: peach, white, butter, mint. */
val NoteColors = listOf(PeachSoft, CreamCard, Color(0xFFFFF1C9), Color(0xFFD6F0E8))

private const val SEARCH_D = "M11 4.5a6.5 6.5 0 1 1 0 13a6.5 6.5 0 1 1 0-13zM16 16l4 4"
private const val FLAG_D = "M6 21V4M6 5h11l-2 4 2 4H6"
private const val PENCIL_D = "M4 20l4-1 11-11-3-3L5 16z"

class NotesViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val notes: StateFlow<List<Note>?> = app.noteRepository.observe(coupleId)
        .catch { Timber.w(it, "Notes unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [onGone]: the edited note was deleted by the partner meanwhile. */
    fun save(me: UserProfile, partner: UserProfile?, id: String?, text: String, color: Int, pinned: Boolean, wasPinned: Boolean, onGone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val repo = app.noteRepository
                if (id == null) {
                    repo.add(coupleId, me, text, color)
                    Telemetry.track(AnalyticsEvent.NoteAdded)
                    if (partner != null) runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Note) }
                } else {
                    repo.update(coupleId, id, text, color)
                    if (pinned != wasPinned) repo.setPinned(coupleId, id, pinned)
                }
            }.onFailure { Timber.w(it, "Saving the note failed"); if (it is ItemGoneException) onGone() }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { app.noteRepository.delete(coupleId, id) }.onFailure { Timber.w(it, "Deleting the note failed") }
        }
    }
}

/** Notlar (approved design 18): filter chips, two-column sticky notes, "Not yaz". */
@Composable
fun NotesScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onSearch: () -> Unit) {
    val app = lovelyyApp()
    val coupleId = me.coupleId ?: return
    val vm: NotesViewModel = viewModel(key = "notes-$coupleId") { NotesViewModel(app, coupleId) }
    val notes by vm.notes.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var tabSel by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 mine, 2 theirs
    var editing by remember { mutableStateOf<Note?>(null) }

    // "Emin misin?" before deleting.
    var askDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    askDelete?.let { act ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.del_note_title),
            body = stringResource(R.string.del_note_body),
            confirm = stringResource(R.string.del_confirm),
            onConfirm = { act(); askDelete = null },
            onDismiss = { askDelete = null; editing = null },
        )
    }
    var creating by rememberSaveable { mutableStateOf(false) }

    val shown = notes.orEmpty().filter {
        when (tabSel) {
            1 -> it.by == me.uid
            2 -> it.by != me.uid
            else -> true
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CenteredTopBar(stringResource(R.string.n_title), onBack) {
                RoundSvgButton(SEARCH_D, stringResource(R.string.n_search), onSearch)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(stringResource(R.string.n_all), stringResource(R.string.n_mine), TurkishSuffix.genitive(partnerName)).forEachIndexed { i, label ->
                    val sel = tabSel == i
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (sel) Brown else CreamCard)
                            .semantics { selected = sel; role = Role.Tab }
                            .bounceClick { tabSel = i }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sel) Cream else Brown, maxLines = 1)
                    }
                }
            }

            if (notes != null && shown.isEmpty()) {
                Text(stringResource(R.string.n_empty), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft, modifier = Modifier.padding(top = 24.dp, start = 4.dp))
            }

            // Two columns, notes dealt left/right in order (pinned first, then newest).
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                listOf(0, 1).forEach { col ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        shown.filterIndexed { i, _ -> i % 2 == col }.forEach { n ->
                            NoteCard(n, mine = n.by == me.uid, myName = me.displayName, partnerName = partnerName) { editing = n }
                        }
                    }
                }
            }
        }

        // "Not yaz", floating just above the bottom menu.
        val shape = RoundedCornerShape(28.dp)
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 122.dp)
                .fillMaxWidth()
                .height(56.dp)
                .softShadow(shape, 12.dp, Peach)
                .clip(shape)
                .background(Peach)
                .semantics { role = Role.Button }
                .bounceClick(pressedScale = 0.97f) { creating = true },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SvgStrokeIcon(PENCIL_D, Brown, Modifier.size(20.dp))
            Text(stringResource(R.string.n_write), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, modifier = Modifier.padding(start = 10.dp))
        }
    }

    // The partner deleted the note that is open here: close the sheet and say so.
    val context = androidx.compose.ui.platform.LocalContext.current
    val goneText = stringResource(R.string.gone_note)
    val showGone = { android.widget.Toast.makeText(context, goneText, android.widget.Toast.LENGTH_SHORT).show() }
    val editingId = editing?.id
    androidx.compose.runtime.LaunchedEffect(notes, editingId) {
        val list = notes
        if (editingId != null && list != null && list.none { it.id == editingId }) { editing = null; showGone() }
    }

    if (creating || editing != null) {
        val n = editing
        NoteEditor(
            note = n,
            onDismiss = { creating = false; editing = null },
            onSave = { text, color, pinned ->
                vm.save(me, partner, n?.id, text, color, pinned, n?.pinned == true, showGone)
                creating = false; editing = null
            },
            onDelete = n?.let { note -> { askDelete = { vm.delete(note.id); editing = null } } },
        )
    }
}

@Composable
private fun NoteCard(n: Note, mine: Boolean, myName: String, partnerName: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(NoteColors[Math.floorMod(n.color, NoteColors.size)])
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (n.pinned) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SvgStrokeIcon(FLAG_D, Brown, Modifier.size(14.dp))
                Text(stringResource(R.string.n_pinned), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown)
            }
        }
        Text(n.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown, maxLines = 12, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val who = if (mine) myName else n.byName.ifBlank { partnerName }
            Box(Modifier.size(22.dp).clip(CircleShape).background(if (mine) Peach else Mint), contentAlignment = Alignment.Center) {
                Text(who.trim().firstOrNull()?.uppercase(TR) ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown)
            }
            Text(noteTime(n.at), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = FootInk)
        }
    }
}

/** "Bugün 14:20", "Dün 23:10", "Pazartesi", "18 Kasım". */
@Composable
private fun noteTime(at: Long): String {
    val dt = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
    val days = ChronoUnit.DAYS.between(dt.toLocalDate(), LocalDate.now())
    val hm = dt.format(DateTimeFormatter.ofPattern("HH:mm", TR))
    return when {
        days <= 0L -> stringResource(R.string.n_today, hm)
        days == 1L -> stringResource(R.string.n_yesterday, hm)
        days < 7L -> dt.format(DateTimeFormatter.ofPattern("EEEE", TR))
        dt.year == LocalDate.now().year -> dt.format(DateTimeFormatter.ofPattern("d MMMM", TR))
        else -> dt.format(DateTimeFormatter.ofPattern("d MMMM yyyy", TR))
    }
}

/** Writing or editing a note: text, color, pin, delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(note: Note?, onDismiss: () -> Unit, onSave: (String, Int, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf(note?.text.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(note?.color?.let { Math.floorMod(it, NoteColors.size) } ?: 0) }
    var pinned by rememberSaveable { mutableStateOf(note?.pinned == true) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (note == null) R.string.n_editor_new else R.string.n_editor_edit),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            )
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(20.dp)
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(NoteRepository.MAX_NOTE) },
                interactionSource = interaction,
                cursorBrush = SolidColor(PeachDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .clip(shape)
                            .background(NoteColors[color])
                            .border(2.dp, if (focused) PeachDeep else FieldBorder, shape)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (text.isEmpty()) Text(stringResource(R.string.n_editor_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft)
                        inner()
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NoteColors.forEachIndexed { i, c ->
                    val label = stringResource(R.string.n_color, i + 1)
                    Box(
                        Modifier
                            .size(44.dp)
                            .then(if (i == color) Modifier.border(3.dp, Brown, CircleShape) else Modifier)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, FieldBorder, CircleShape)
                            .semantics { contentDescription = label; selected = i == color; role = Role.RadioButton }
                            .bounceClick { color = i },
                    )
                }
                Box(Modifier.weight(1f))
                if (note != null) {
                    SmallChip(stringResource(if (pinned) R.string.n_unpin else R.string.n_pin), CreamCard) { pinned = !pinned }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    SmallChip(stringResource(R.string.n_delete), Color(0xFFF6EEE8), Modifier.height(56.dp)) {
                        scope.launch { sheet.hide() }.invokeOnCompletion { onDelete() }
                    }
                }
                val btn = RoundedCornerShape(28.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(btn)
                        .background(if (text.isNotBlank()) Peach else PeachSoft)
                        .semantics { role = Role.Button }
                        .bounceClick(enabled = text.isNotBlank(), pressedScale = 0.97f) {
                            scope.launch { sheet.hide() }.invokeOnCompletion { onSave(text, color, pinned) }
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.n_save), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown) }
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .semantics { role = Role.Button }
            .bounceClick(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1, softWrap = false) }
}
