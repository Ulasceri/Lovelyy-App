package com.lovelyy.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.Capsule
import com.lovelyy.app.data.Dream
import com.lovelyy.app.data.Note
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.dreams.categoryLabel
import com.lovelyy.app.ui.history.*
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import java.time.LocalDate

private const val BACK_D = "M15 5l-7 7 7 7"
private const val NOTE_D = "M6 4h9l4 4v12H6z M14 4v5h5 M9 13h6M9 16h4"
private const val STAR_D = "M12 4l2.5 5 5.5.8-4 3.9.9 5.5-4.9-2.6-4.9 2.6.9-5.5-4-3.9 5.5-.8z"
private const val CHEVRON_D = "M9 6l6 6-6 6"

/** Which list the search starts on (the screen it was opened from). */
enum class SearchScope {
    All, Notes, Dreams, Memories;

    companion object {
        /** From the route ("search:notes"); the Anılar tab searches everything. */
        fun of(key: String) = when (key) {
            "notes" -> Notes
            "dreams" -> Dreams
            else -> All
        }
    }
}

/** Arama (boards 24-25): one search over notes, dreams and memories (opened letters). Runs on the phone. */
@Composable
fun SearchScreen(
    me: UserProfile,
    partner: UserProfile?,
    start: SearchScope,
    onBack: () -> Unit,
    onNote: (String) -> Unit,
    onDream: (String) -> Unit,
    onLetter: (String) -> Unit,
) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val notes by remember(coupleId) { app.noteRepository.observe(coupleId).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(emptyList())
    val dreams by remember(coupleId) { app.dreamRepository.observe(coupleId).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(emptyList())
    val capsules by remember(coupleId) { app.capsuleRepository.observe(coupleId).catch { emit(emptyList()) } }.collectAsStateWithLifecycle(emptyList())
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(start) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val q = query.trim()
    val today = LocalDate.now()
    fun hit(s: String) = q.isNotEmpty() && s.contains(q, ignoreCase = true)
    val noteHits = notes.filter { hit(it.text) }
    val dreamHits = dreams.filter { hit(it.title) || hit(it.goal) }
    // Sealed letters stay secret: only opened ones are searchable.
    val letterHits = capsules.filter { it.isOpen(today) && (hit(it.title) || hit(it.text)) }
    val total = noteHits.size + dreamHits.size + letterHits.size

    InnerColumn(spacing = 12) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundSvgButton(BACK_D, stringResource(R.string.action_back), onBack)
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SvgStrokeIcon(Kit.SEARCH_D, Muted, Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.se_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        query, { query = it.take(40) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brown),
                        cursorBrush = SolidColor(Brown),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Kit.ButtonIdle).bounceClick { query = "" }, contentAlignment = Alignment.Center) {
                        SvgStrokeIcon(Kit.X_D, Brown, Modifier.size(14.dp))
                    }
                }
            }
        }
        val counts = listOf(total, noteHits.size, dreamHits.size, letterHits.size)
        FilterChips(
            listOf(R.string.hi_all, R.string.se_notes, R.string.se_dreams, R.string.se_memories).mapIndexed { i, r ->
                stringResource(r) + if (q.isNotEmpty()) " ${counts[i]}" else ""
            },
            scope.ordinal, { scope = SearchScope.entries[it] },
        )

        when {
            q.isEmpty() -> EmptyNote(stringResource(R.string.se_start))
            total == 0 || (scope != SearchScope.All && counts[scope.ordinal] == 0) -> NoResults(q) { query = "" }
            else -> {
                Text(
                    pluralStringResource(R.plurals.se_count, (counts[scope.ordinal]).toInt(), q, counts[scope.ordinal]),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SubText, modifier = Modifier.padding(start = 4.dp),
                )
                if ((scope == SearchScope.All || scope == SearchScope.Notes) && noteHits.isNotEmpty()) {
                    DayHeader(stringResource(R.string.se_notes))
                    noteHits.forEach { n -> NoteHit(n, q, n.by == me.uid, me.displayName, partnerName) { onNote(n.id) } }
                }
                if ((scope == SearchScope.All || scope == SearchScope.Dreams) && dreamHits.isNotEmpty()) {
                    DayHeader(stringResource(R.string.se_dreams))
                    dreamHits.forEach { d -> DreamHit(d, q) { onDream(d.id) } }
                }
                if ((scope == SearchScope.All || scope == SearchScope.Memories) && letterHits.isNotEmpty()) {
                    DayHeader(stringResource(R.string.se_memories))
                    letterHits.forEach { c -> LetterHit(c, q, c.by == me.uid, partnerName) { onLetter(c.id) } }
                }
            }
        }
    }
}

/** Marks every match of [q] in butter yellow. */
private fun highlight(text: String, q: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val j = text.indexOf(q, i, ignoreCase = true)
        if (j < 0 || q.isEmpty()) { append(text.substring(i)); break }
        append(text.substring(i, j))
        pushStyle(SpanStyle(background = Highlight))
        append(text.substring(j, j + q.length))
        pop()
        i = j + q.length
    }
}

/** A window of [text] around the first match, so long notes show the matching part. */
private fun snippet(text: String, q: String, max: Int = 90): String {
    val flat = text.replace('\n', ' ')
    val at = flat.indexOf(q, ignoreCase = true)
    if (flat.length <= max || at < 30) return flat
    val start = (at - 30).coerceAtLeast(0)
    return "…" + flat.substring(start)
}

@Composable
private fun HitText(text: String, q: String, size: Int = 15) {
    Text(highlight(snippet(text, q), q), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size + 5).sp, color = Brown, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun NoteHit(n: Note, q: String, mine: Boolean, myName: String, partnerName: String, onClick: () -> Unit) {
    HistoryRow(
        icon = { SvgStrokeIcon(NOTE_D, Brown, Modifier.size(22.dp)) },
        iconBg = Color(0xFFFFF1C9),
        title = "",
        subtitle = null,
        onClick = onClick,
        trailing = { SvgStrokeIcon(CHEVRON_D, Muted, Modifier.size(16.dp)) },
        extra = {
            HitText(n.text, q)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniAvatar(if (mine) myName else partnerName, if (mine) Peach else Mint)
                Text(
                    stringResource(if (mine) R.string.se_by_me else R.string.se_by, n.byName.ifBlank { partnerName }) + " · " + n.at.localDate().format(DAY_MONTH),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubText,
                )
            }
        },
    )
}

@Composable
private fun DreamHit(d: Dream, q: String, onClick: () -> Unit) {
    HistoryRow(
        icon = { SvgStrokeIcon(STAR_D, Brown, Modifier.size(22.dp)) },
        iconBg = Color(0xFFFFE6D6),
        title = "",
        subtitle = null,
        onClick = onClick,
        trailing = { SvgStrokeIcon(CHEVRON_D, Muted, Modifier.size(16.dp)) },
        extra = {
            HitText(d.title, q)
            Text(
                listOfNotNull(categoryLabel(d.category), d.goal.takeIf { it.isNotBlank() }, stringResource(if (d.doneAt != null) R.string.se_dream_done else R.string.se_dream_next)).joinToString(" · "),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted,
            )
        },
    )
}

@Composable
private fun LetterHit(c: Capsule, q: String, mine: Boolean, partnerName: String, onClick: () -> Unit) {
    HistoryRow(
        icon = { SvgStrokeIcon(Kit.ENVELOPE_D, Brown, Modifier.size(22.dp)) },
        iconBg = Color(0xFFD6F0E8),
        title = "",
        subtitle = null,
        onClick = onClick,
        trailing = { SvgStrokeIcon(CHEVRON_D, Muted, Modifier.size(16.dp)) },
        extra = {
            HitText(if (c.title.contains(q, true)) c.title else c.text, q)
            Text(
                stringResource(if (mine) R.string.se_letter_me else R.string.se_letter_by, c.byName.ifBlank { partnerName }) + " · " + c.openOn.format(DAY_MONTH_YEAR),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted,
            )
        },
    )
}

@Composable
private fun NoResults(q: String, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            SvgStrokeIcon(Kit.SEARCH_D, Muted, Modifier.size(40.dp))
        }
        Text(stringResource(R.string.se_none_title, q), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown, textAlign = TextAlign.Center)
        Text(stringResource(R.string.se_none_body), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SubText, textAlign = TextAlign.Center)
        MainAction(stringResource(R.string.se_clear), Kit.X_D, onClick = onClear, modifier = Modifier.padding(top = 8.dp))
    }
}
