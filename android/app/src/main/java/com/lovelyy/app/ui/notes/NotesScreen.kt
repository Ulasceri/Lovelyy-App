package com.lovelyy.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.NOTE_COLORS
import com.lovelyy.app.data.Note
import com.lovelyy.app.data.NoteRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.home.ComingSoon
import com.lovelyy.app.ui.home.relativeTime
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Sticky-note colors: peach, mint, butter, lilac, sky. */
val NotePalette = listOf(Color(0xFFFFE6D6), Color(0xFFDDF2EC), Color(0xFFFFF2C6), Color(0xFFEDE4FA), Color(0xFFE3F1FA))

private fun noteColor(i: Int) = NotePalette[i.coerceIn(0, NotePalette.lastIndex)]

/** Notlar: shared sticky notes, both partners can add, edit, pin and delete. */
@Composable
fun NotesScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val scope = rememberCoroutineScope()
    val coupleId = me.coupleId ?: return
    val notes by remember(coupleId) { app.noteRepository.observe(coupleId).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(null)
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 110.dp)) {
            ScreenTitle(stringResource(R.string.feature_notes), onBack)
            val list = notes
            when {
                list == null -> Unit
                list.isEmpty() -> ComingSoon(
                    stringResource(R.string.notes_empty_title), stringResource(R.string.notes_empty_body), Icons.Rounded.EditNote,
                    Modifier.padding(top = 24.dp),
                )
                else -> {
                    Text(
                        stringResource(R.string.notes_count, list.size),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    )
                    // Two staggered columns, like a fridge door.
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0..1) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                list.filterIndexed { i, _ -> i % 2 == col }.forEach { n ->
                                    NoteCard(n, me.uid, partner?.displayName.orEmpty(), tilt = if ((n.id.hashCode() and 1) == 0) -1.2f else 1.2f) { editing = n }
                                }
                            }
                        }
                    }
                }
            }
        }
        PeachButton(
            stringResource(R.string.notes_add), { creating = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            leading = { Icon(Icons.Rounded.Add, null, tint = Brown, modifier = Modifier.size(22.dp)) },
        )
    }

    if (creating || editing != null) {
        val n = editing
        NoteEditor(
            initial = n,
            onDismiss = { creating = false; editing = null },
            onSave = { text, color ->
                creating = false; editing = null
                scope.launch {
                    runCatching {
                        if (n == null) {
                            app.noteRepository.add(coupleId, me, text, color)
                            partner?.let { app.signalRepository.send(coupleId, me, it.uid, SignalKind.Note) }
                        } else {
                            app.noteRepository.update(coupleId, n.id, text, color)
                        }
                    }
                }
            },
            onPin = n?.let { note -> { scope.launch { runCatching { app.noteRepository.setPinned(coupleId, note.id, !note.pinned) } }; editing = null } },
            onDelete = n?.let { note -> { scope.launch { runCatching { app.noteRepository.delete(coupleId, note.id) } }; editing = null } },
        )
    }
}

@Composable
private fun NoteCard(n: Note, myUid: String, partnerName: String, tilt: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .rotate(tilt)
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(noteColor(n.color))
            .bounceClick(pressedScale = 0.97f, onClick = onClick)
            .padding(14.dp),
    ) {
        if (n.pinned) Icon(Icons.Rounded.PushPin, null, tint = PeachDeep, modifier = Modifier.size(16.dp).padding(bottom = 2.dp))
        Text(n.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown, maxLines = 10, overflow = TextOverflow.Ellipsis)
        Text(
            (if (n.by == myUid) stringResource(R.string.cat_you) else n.byName.ifBlank { partnerName }) + " · " + relativeTime(n.at),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrownSoft, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun NoteEditor(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onPin: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var color by remember { mutableIntStateOf(initial?.color ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .imePadding()
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Cream)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (initial == null) R.string.notes_new else R.string.notes_edit),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.weight(1f),
                )
                if (onPin != null) CircleIconButton(Icons.Rounded.PushPin, stringResource(R.string.notes_pin), onPin, tint = if (initial?.pinned == true) PeachDeep else Brown)
                if (onDelete != null) {
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.notes_delete), { if (confirmDelete) onDelete() else confirmDelete = true })
                }
            }
            if (confirmDelete) {
                Text(stringResource(R.string.notes_delete_confirm), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PeachDeep, modifier = Modifier.padding(top = 8.dp))
            }
            val shape = RoundedCornerShape(20.dp)
            BasicTextField(
                value = text, onValueChange = { text = it.take(NoteRepository.MAX_NOTE) },
                textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Brown),
                cursorBrush = SolidColor(PeachDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().heightIn(min = 160.dp).clip(shape).background(noteColor(color)).padding(16.dp)) {
                        if (text.isEmpty()) Text(stringResource(R.string.notes_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF9A8A83))
                        inner()
                    }
                },
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until NOTE_COLORS) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(noteColor(i))
                            .border(if (i == color) 2.5.dp else 1.dp, if (i == color) Brown else Color(0xFFE6DCD4), CircleShape)
                            .bounceClick { color = i },
                        contentAlignment = Alignment.Center,
                    ) { if (i == color) Icon(Icons.Rounded.Check, null, tint = Brown, modifier = Modifier.size(18.dp)) }
                }
            }
            PeachButton(stringResource(R.string.action_save), { onSave(text, color) }, enabled = text.isNotBlank(), modifier = Modifier.padding(top = 18.dp))
            Text(
                stringResource(R.string.action_cancel), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrownSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).bounceClick(onClick = onDismiss).padding(12.dp),
            )
        }
    }
}
