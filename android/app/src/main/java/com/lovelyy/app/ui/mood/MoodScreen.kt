package com.lovelyy.app.ui.mood

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.MoodEntry
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.data.MoodRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CLOCK_D
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.heartbeat.timeAgo
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private val NoteBubble = Color(0xFFE6F5F0)
private val NoteLabel = Color(0xFF5F524C)
private val FieldBorder = Color(0xFFF0E2D8)

class MoodViewModel(private val app: LovelyyApplication) : ViewModel() {
    /** Saves my mood and tells my partner. True when the mood was saved. */
    suspend fun share(me: UserProfile, partner: UserProfile?, kind: MoodKind, note: String): Boolean {
        val coupleId = me.coupleId ?: return false
        val saved = runCatching { app.moodRepository.set(coupleId, me.uid, kind, note) }
            .onFailure { Timber.w(it, "Saving the mood failed") }
            .isSuccess
        if (!saved) return false
        Telemetry.track(AnalyticsEvent.MoodShared(kind.key, note.isNotBlank()))
        if (partner != null) {
            runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Mood, mood = kind) }
                .onFailure { Timber.w(it, "Mood notice to partner failed") }
        }
        return true
    }
}

/** Ruh hali (approved design 7): both moods on top, pick a face, add a note, share. */
@Composable
fun MoodScreen(me: UserProfile, partner: UserProfile?, moods: Map<String, MoodEntry>, onBack: () -> Unit, onHistory: () -> Unit) {
    val app = lovelyyApp()
    val vm: MoodViewModel = viewModel { MoodViewModel(app) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val mine = moods[me.uid]
    val theirs = partner?.uid?.let { moods[it] }

    var pick by rememberSaveable { mutableStateOf((mine?.kind?.takeIf { it in PickerMoods } ?: MoodKind.Mutlu).key) }
    val sel = MoodKind.of(pick) ?: MoodKind.Mutlu
    var note by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf(0) } // 0 ready, 1 sending, 2 shared, 3 failed
    LaunchedEffect(state) { if (state >= 2) { delay(2200); state = 0 } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
                .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CenteredTopBar(stringResource(R.string.md_title), onBack) {
            RoundSvgButton(CLOCK_D, stringResource(R.string.md_history), onHistory)
        }

        // You and your partner right now.
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val bob by rememberInfiniteTransition(label = "bob").animateFloat(
                    0f, 0f, infiniteRepeatable(keyframes { durationMillis = 3000; 0f at 0; -3f at 1500; 0f at 3000 }), label = "y",
                )
                val yourCd = stringResource(R.string.md_your_mood_cd)
                Person(
                    face = { MoodFace(sel, Modifier.size(56.dp).offset { androidx.compose.ui.unit.IntOffset(0, bob.dp.roundToPx()) }.semantics { contentDescription = yourCd }) },
                    who = stringResource(R.string.md_you),
                    label = stringResource(sel.labelRes()),
                    time = if (mine != null && mine.kind == sel) timeAgo(mine.at) else stringResource(R.string.md_now),
                    modifier = Modifier.weight(1f),
                )
                val theirLabel = theirs?.let { stringResource(it.kind.labelRes()) } ?: stringResource(R.string.md_partner_none)
                val theirCd = stringResource(R.string.md_partner_mood_cd, TurkishSuffix.genitive(partnerName), theirLabel)
                Person(
                    face = { MoodFace(theirs?.kind, Modifier.size(56.dp).semantics { contentDescription = theirCd }) },
                    who = partnerName,
                    label = theirLabel,
                    time = theirs?.let { timeAgo(it.at) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (mine != null && mine.note.isNotBlank()) {
                NoteBox(stringResource(R.string.md_my_note), mine.note)
            }
            if (theirs != null && theirs.note.isNotBlank()) {
                NoteBox(stringResource(R.string.md_partner_note, TurkishSuffix.genitive(partnerName)), theirs.note)
            }
        }

        Text(stringResource(R.string.md_question), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.padding(start = 2.dp))

        // 4 × 2 faces.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerMoods.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { kind ->
                        val isSel = kind == sel
                        Column(
                            Modifier
                                .weight(1f)
                                .semantics { selected = isSel; role = Role.RadioButton }
                                .bounceClick { pick = kind.key }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(68.dp).then(if (isSel) Modifier.border(3.dp, Brown, CircleShape) else Modifier).padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) { MoodFace(kind, Modifier.size(62.dp)) }
                            Text(stringResource(kind.labelRes()), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Optional note.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.md_note_label), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.padding(start = 4.dp))
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val fieldShape = RoundedCornerShape(18.dp)
            BasicTextField(
                value = note,
                onValueChange = { note = it.take(MoodRepository.MAX_NOTE) },
                singleLine = true,
                interactionSource = interaction,
                cursorBrush = SolidColor(PeachDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brown),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(fieldShape)
                            .background(CreamCard)
                            .border(2.dp, if (focused) PeachDeep else FieldBorder, fieldShape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (note.isEmpty()) {
                            Text(
                                stringResource(R.string.md_note_hint, TurkishSuffix.dative(partnerName)),
                                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
        }

        // Share.
        val btnShape = RoundedCornerShape(28.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .softShadow(btnShape, 12.dp, Peach)
                .clip(btnShape)
                .background(Peach)
                .semantics { role = Role.Button }
                .bounceClick(enabled = state == 0, pressedScale = 0.97f) {
                    focus.clearFocus()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state = 1
                    scope.launch {
                        val ok = vm.share(me, partner, sel, note)
                        if (ok) note = ""
                        state = if (ok) 2 else 3
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SvgIcon(HEART_D, Brown, Modifier.size(20.dp))
            Text(
                when (state) {
                    2 -> stringResource(R.string.md_shared, TurkishSuffix.dative(partnerName))
                    3 -> stringResource(R.string.md_failed)
                    else -> stringResource(R.string.md_share, partnerName)
                },
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun NoteBox(label: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
            .background(NoteBubble)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NoteLabel)
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Brown)
    }
}

@Composable
private fun Person(face: @Composable () -> Unit, who: String, label: String, time: String?, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        face()
        Column {
            Text(who, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (time != null) Text(time, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, maxLines = 1)
        }
    }
}
