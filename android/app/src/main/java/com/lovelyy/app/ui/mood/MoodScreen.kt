package com.lovelyy.app.ui.mood

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.MoodEntry
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.data.MoodRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.home.relativeTime
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintInkSoft
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun moodLabel(kind: MoodKind): String = stringResource(kind.labelRes())

/** Ruh hali: pick how you feel (+ an optional note); your partner sees it and gets a notice. */
@Composable
fun MoodScreen(me: UserProfile, partner: UserProfile?, moods: Map<String, MoodEntry>, onBack: () -> Unit) {
    val app = lovelyyApp()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val mine = moods[me.uid]
    val theirs = partner?.uid?.let { moods[it] }

    var selected by rememberSaveable { mutableStateOf(mine?.kind?.key) }
    var note by rememberSaveable { mutableStateOf(mine?.note.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) { delay(1800); saved = false } }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        ScreenTitle(stringResource(R.string.feature_mood), onBack)

        // Partner's mood first: that's what you came to see.
        SoftCard(Modifier.fillMaxWidth().padding(top = 18.dp), color = MintSoft, radius = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(theirs?.kind?.emoji ?: "🫥", fontSize = 48.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.mood_partner_now, partnerName), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MintInkSoft)
                    Text(
                        theirs?.let { moodLabel(it.kind) } ?: stringResource(R.string.mood_partner_none),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MintInk,
                    )
                    if (theirs != null) {
                        Text(relativeTime(theirs.at), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MintInkSoft)
                    }
                }
            }
            if (!theirs?.note.isNullOrBlank()) {
                Text(
                    "“${theirs!!.note}”",
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = MintInk,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CreamCard.copy(alpha = 0.7f)).padding(12.dp),
                )
            }
        }

        Text(
            stringResource(R.string.mood_you_title),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
        )
        MoodKind.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { k ->
                    MoodChip(k, k.key == selected, Modifier.weight(1f)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = k.key
                    }
                }
            }
        }

        // Optional note.
        val shape = RoundedCornerShape(18.dp)
        BasicTextField(
            value = note,
            onValueChange = { note = it.take(MoodRepository.MAX_NOTE) },
            textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown),
            cursorBrush = SolidColor(PeachDeep),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            decorationBox = { inner ->
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(shape).background(CreamCard).border(1.5.dp, Outline, shape).padding(14.dp),
                ) {
                    Box(Modifier.weight(1f, fill = false)) {
                        if (note.isEmpty()) Text(stringResource(R.string.mood_note_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF9A8A83))
                        inner()
                    }
                    Text(
                        "${note.length}/${MoodRepository.MAX_NOTE}",
                        fontFamily = Quicksand, fontSize = 11.sp, color = BrownSoft, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            },
        )

        Spacer(Modifier.padding(top = 18.dp))
        PeachButton(
            stringResource(if (saved) R.string.mood_shared else R.string.mood_share, partnerName),
            enabled = selected != null,
            loading = saving,
            onClick = {
                val kind = MoodKind.of(selected) ?: return@PeachButton
                val coupleId = me.coupleId ?: return@PeachButton
                saving = true
                scope.launch {
                    val ok = runCatching { app.moodRepository.set(coupleId, me.uid, kind, note) }.isSuccess
                    if (ok && partner != null) {
                        runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Mood, mood = kind) }
                    }
                    saving = false
                    saved = ok
                }
            },
        )
        if (mine != null) {
            Text(
                stringResource(R.string.mood_yours_now, moodLabel(mine.kind), mine.kind.emoji, relativeTime(mine.at)),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun MoodChip(kind: MoodKind, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val bg by animateColorAsState(if (selected) PeachTint else CreamCard, label = "bg")
    val pop by animateFloatAsState(if (selected) 1.15f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "pop")
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) Peach else Color(0xFFF0E6DE), shape)
            .bounceClick(haptic = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(kind.emoji, fontSize = 28.sp, modifier = Modifier.scale(pop))
        Text(
            moodLabel(kind), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
            maxLines = 1, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
