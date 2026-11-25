package com.lovelyy.app.ui.question

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.Answer
import com.lovelyy.app.data.DayQuestion
import com.lovelyy.app.data.QuestionBank
import com.lovelyy.app.data.QuestionRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.ScreenTitle
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintInkSoft
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachCard
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private fun dayTitle(day: String): String =
    runCatching { LocalDate.parse(day).format(DateTimeFormatter.ofPattern("d MMMM, EEEE", TR)) }.getOrDefault(day)

/** Günün sorusu: answer today's question; the partner's answer opens once you answered too. */
@Composable
fun QuestionScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val app = lovelyyApp()
    val scope = rememberCoroutineScope()
    val coupleId = me.coupleId ?: return
    val today = remember { LocalDate.now() }
    val recent by remember(coupleId) { app.questionRepository.observeRecent(coupleId).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(emptyList())
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)

    val todayDoc = recent.firstOrNull { it.day == today.toString() }
    val mine = todayDoc?.answers?.get(me.uid)
    val theirs = partner?.uid?.let { todayDoc?.answers?.get(it) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
    ) {
        ScreenTitle(stringResource(R.string.feature_question), onBack)

        SoftCard(Modifier.fillMaxWidth().padding(top = 18.dp), color = PeachCard, radius = 28.dp, padding = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QuestionAnswer, null, tint = PeachDeep, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(dayTitle(today.toString()), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
            }
            Text(
                QuestionBank.forDay(today),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp, color = Brown,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        AnimatedContent(mine == null || editing, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "answer") { writing ->
            Column {
                if (writing) {
                    PartnerStatus(theirs != null, partnerName, Modifier.padding(top = 16.dp))
                    AnswerField(text, { text = it.take(QuestionRepository.MAX_ANSWER) }, Modifier.padding(top = 14.dp))
                    PeachButton(
                        stringResource(if (theirs != null) R.string.q_answer_and_see else R.string.q_answer),
                        enabled = text.isNotBlank(), loading = saving,
                        modifier = Modifier.padding(top = 14.dp),
                        onClick = {
                            saving = true
                            scope.launch {
                                val first = mine == null
                                val ok = runCatching { app.questionRepository.answer(coupleId, today, me, text) }.isSuccess
                                if (ok && first && partner != null) {
                                    runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Question) }
                                }
                                saving = false
                                if (ok) editing = false
                            }
                        },
                    )
                } else {
                    AnswerBubble(stringResource(R.string.cat_you), mine!!.text, PeachTint, BrownSoft, Modifier.padding(top = 16.dp))
                    if (theirs != null) AnswerBubble(partnerName, theirs.text, MintSoft, MintInkSoft, Modifier.padding(top = 10.dp))
                    else Locked(stringResource(R.string.q_waiting, partnerName), Modifier.padding(top = 10.dp))
                    TextButton(
                        onClick = { text = mine.text; editing = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    ) { Text(stringResource(R.string.q_edit), fontFamily = Quicksand, fontWeight = FontWeight.Bold, color = BrownSoft) }
                }
            }
        }

        val past = recent.filter { it.day != today.toString() }
        if (past.isNotEmpty()) {
            Text(
                stringResource(R.string.q_past), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
            past.forEach { PastCard(it, me.uid, partner?.uid, partnerName, Modifier.padding(bottom = 10.dp)) }
        }
    }
}

@Composable
private fun PartnerStatus(answered: Boolean, partnerName: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(if (answered) MintSoft else Color(0xFFF6EEE8)).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (answered) Mint else Outline), contentAlignment = Alignment.Center) {
            Text(partnerName.firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(if (answered) R.string.q_partner_answered else R.string.q_partner_not_yet, partnerName),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (answered) MintInk else BrownSoft,
        )
    }
}

@Composable
private fun AnswerField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Brown),
        cursorBrush = SolidColor(PeachDeep),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(shape).background(CreamCard).border(1.5.dp, Outline, shape).padding(16.dp)) {
                if (value.isEmpty()) Text(stringResource(R.string.q_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF9A8A83))
                inner()
            }
        },
    )
}

@Composable
private fun AnswerBubble(who: String, text: String, bg: Color, labelColor: Color, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(16.dp)) {
        Text(who, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = labelColor)
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Brown, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Locked(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFF6EEE8)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Lock, null, tint = BrownSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
    }
}

@Composable
private fun PastCard(q: DayQuestion, myUid: String, partnerUid: String?, partnerName: String, modifier: Modifier = Modifier) {
    val mine: Answer? = q.answers[myUid]
    val theirs: Answer? = partnerUid?.let { q.answers[it] }
    SoftCard(modifier.fillMaxWidth(), padding = 16.dp) {
        Text(dayTitle(q.day), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft)
        Text(q.question, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, color = Brown, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mine != null) AnswerBubble(stringResource(R.string.cat_you), mine.text, PeachTint, BrownSoft)
            when {
                theirs == null -> Text(stringResource(R.string.q_partner_skipped, partnerName), fontFamily = Quicksand, fontSize = 13.sp, color = BrownSoft)
                mine == null -> Locked(stringResource(R.string.q_locked_past, partnerName))
                else -> AnswerBubble(partnerName, theirs.text, MintSoft, MintInkSoft)
            }
        }
    }
}

/** Home card: today's question and who answered. */
@Composable
fun QuestionHomeCard(me: UserProfile, partner: UserProfile?, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val app = lovelyyApp()
    val coupleId = me.coupleId ?: return
    val today = remember { LocalDate.now() }
    val recent by remember(coupleId) { app.questionRepository.observeRecent(coupleId, 1).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(emptyList())
    val doc = recent.firstOrNull { it.day == today.toString() }
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val iAnswered = doc?.answers?.containsKey(me.uid) == true
    val theyAnswered = partner?.uid?.let { doc?.answers?.containsKey(it) } == true
    SoftCard(modifier.fillMaxWidth(), radius = 26.dp, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.QuestionAnswer, null, tint = PeachDeep, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.feature_question), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrownSoft)
        }
        Text(QuestionBank.forDay(today), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.sp, color = Brown, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(stringResource(if (iAnswered) R.string.q_you_answered else R.string.q_your_turn), iAnswered)
            StatusChip(stringResource(if (theyAnswered) R.string.q_partner_answered_short else R.string.q_partner_not_yet_short, partnerName), theyAnswered)
        }
        Box(
            Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(Peach),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(
                    when {
                        !iAnswered && theyAnswered -> R.string.q_cta_see
                        !iAnswered -> R.string.q_answer
                        else -> R.string.q_cta_open
                    },
                ),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, done: Boolean) {
    Text(
        (if (done) "✓ " else "") + text,
        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        color = if (done) MintInk else BrownSoft,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (done) MintSoft else Color(0xFFF6EEE8)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
