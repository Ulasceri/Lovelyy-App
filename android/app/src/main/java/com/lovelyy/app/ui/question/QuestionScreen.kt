package com.lovelyy.app.ui.question

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayQuestion
import com.lovelyy.app.data.QuestionBank
import com.lovelyy.app.data.QuestionMath
import com.lovelyy.app.data.QuestionRepository
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CLOCK_D
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
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
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val Skeleton = Color(0xFFF3EAE3)
private val FieldBorder = Color(0xFFF0E2D8)
private val StarInk = Color(0xFFB5532A)

private const val STAR_D = "M12 4l2.5 5 5.5.8-4 3.9.9 5.5-4.9-2.6-4.9 2.6.9-5.5-4-3.9 5.5-.8z"
private const val BUBBLE_D = "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7l-5 4v-4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
private const val LOCK_D = "M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 17.5v-4A2.5 2.5 0 0 1 7.5 11zM8 11V8a4 4 0 0 1 8 0v3"

class QuestionViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val days: StateFlow<List<DayQuestion>?> = app.questionRepository.observeRecent(coupleId)
        .catch { Timber.w(it, "Questions unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun answer(me: UserProfile, partner: UserProfile?, text: String): Boolean {
        val ok = runCatching { app.questionRepository.answer(coupleId, com.lovelyy.app.data.CoupleClock.today(), me, text) }
            .onFailure { Timber.w(it, "Saving the answer failed") }
            .isSuccess
        if (!ok) return false
        Telemetry.track(AnalyticsEvent.QuestionAnswered)
        if (partner != null) {
            runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Question) }
                .onFailure { Timber.w(it, "Question notice to partner failed") }
        }
        return true
    }
}

/** Günün sorusu (approved design 8). */
@Composable
fun QuestionScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBack: () -> Unit, onHistory: () -> Unit) {
    val app = lovelyyApp()
    val coupleId = couple?.id ?: me.coupleId ?: return
    val vm: QuestionViewModel = viewModel(key = "question-$coupleId") { QuestionViewModel(app, coupleId) }
    val days by vm.days.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)

    val today = com.lovelyy.app.ui.coupleToday()
    val todayDoc = days?.firstOrNull { it.day == today.toString() }
    val mine = todayDoc?.answers?.get(me.uid)
    val theirs = partner?.uid?.let { todayDoc?.answers?.get(it) }
    val streak = days?.let { QuestionMath.streak(it, today) } ?: 0
    // "48. soru": how many days the couple has been on Lovelyy (one question a day).
    val number = couple?.createdAt?.toDate()?.time
        ?.let { ChronoUnit.DAYS.between(com.lovelyy.app.data.DayCounter.dateOf(it), today).toInt() + 1 }
        ?: (QuestionBank.QUESTIONS.indexOf(QuestionBank.forDay(today)) + 1)

    var text by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mine?.text) { if (!loaded && mine != null) { text = mine.text; loaded = true } }
    var state by remember { mutableStateOf(0) } // 0 ready, 1 sending, 3 failed
    LaunchedEffect(state) { if (state == 3) { delay(2200); state = 0 } }
    val answered = mine != null
    // Once both answered, the answers are locked: no more edits (the repository enforces it too).
    val locked = QuestionMath.isLocked(todayDoc?.answers?.keys.orEmpty(), me.uid)
    LaunchedEffect(locked, mine?.text) { if (locked && mine != null) text = mine.text }
    val unchanged = answered && text.trim() == mine?.text

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CenteredTopBar(stringResource(R.string.feature_question), onBack) {
            RoundSvgButton(CLOCK_D, stringResource(R.string.q_history), onHistory)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.q_chip, today.format(DateTimeFormatter.ofPattern("d MMMM", TR)), number),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(PeachSoft).padding(horizontal = 12.dp, vertical = 7.dp),
            )
            Box(Modifier.weight(1f))
            SvgStrokeIcon(STAR_D, StarInk, Modifier.size(16.dp))
            Text(
                stringResource(R.string.q_streak, streak),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, modifier = Modifier.padding(start = 6.dp),
            )
        }

        val qShape = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(qShape, 12.dp).clip(qShape).background(PeachSoft).padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SvgStrokeIcon(BUBBLE_D, Brown, Modifier.size(26.dp))
            Text(
                todayDoc?.question ?: QuestionBank.forDay(today),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 31.sp, color = Brown,
                modifier = Modifier.semantics { heading() },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.q_your_answer), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.padding(start = 4.dp))
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(20.dp)
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(QuestionRepository.MAX_ANSWER) },
                readOnly = locked,
                interactionSource = interaction,
                cursorBrush = SolidColor(PeachDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(shape)
                            .background(CreamCard)
                            .border(2.dp, if (focused) PeachDeep else FieldBorder, shape)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (text.isEmpty()) Text(stringResource(R.string.q_answer_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft)
                        inner()
                    }
                },
            )
        }

        // The partner's answer: locked until you answered.
        val cShape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(cShape, 6.dp).clip(cShape).background(CreamCard).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Mint).border(3.dp, Cream, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(partnerName.trim().firstOrNull()?.uppercase(TR) ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown) }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.q_partner_answer, TurkishSuffix.genitive(partnerName)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                    Text(
                        theirs?.let { stringResource(R.string.q_partner_answered, timeAgo(it.at)) } ?: stringResource(R.string.q_partner_waiting),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                    )
                }
                if (!answered) SvgStrokeIcon(LOCK_D, BrownSoft, Modifier.size(20.dp))
            }
            when {
                !answered -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f, 0.86f, 0.6f).forEach { w ->
                        Box(Modifier.fillMaxWidth(w).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Skeleton))
                    }
                    Text(stringResource(R.string.q_locked), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
                }
                theirs != null -> Text(theirs.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, color = Brown)
                else -> Text(stringResource(R.string.q_partner_pending), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
            }
        }

        if (locked) {
            Text(
                stringResource(R.string.q_answers_locked),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrownSoft,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            val btnShape = RoundedCornerShape(28.dp)
            val canSend = state == 0 && text.isNotBlank() && !unchanged
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .softShadow(btnShape, 12.dp, Peach)
                    .clip(btnShape)
                    .background(Peach)
                    .semantics { role = Role.Button }
                    .bounceClick(enabled = canSend, pressedScale = 0.97f) {
                        focus.clearFocus()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state = 1
                        scope.launch {
                            val ok = vm.answer(me, partner, text)
                            state = if (ok) 0 else 3
                            if (ok) text = text.trim()
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SvgIcon(HEART_D, Brown, Modifier.size(20.dp))
                Text(
                    when {
                        state == 3 -> stringResource(R.string.q_failed)
                        unchanged -> stringResource(R.string.q_sent, TurkishSuffix.dative(partnerName))
                        else -> stringResource(R.string.q_send)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}
