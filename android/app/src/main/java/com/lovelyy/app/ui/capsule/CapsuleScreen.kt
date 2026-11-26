package com.lovelyy.app.ui.capsule

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.lovelyy.app.data.Capsule
import com.lovelyy.app.data.CapsuleDraft
import com.lovelyy.app.data.CapsuleRepository
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CLOCK_D
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.LovelyyDatePicker
import com.lovelyy.app.ui.components.MainAction
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SheetField
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.TagChip
import com.lovelyy.app.ui.components.WhiteCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
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
private val DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)

class CapsuleViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val capsules: StateFlow<List<Capsule>?> = app.capsuleRepository.observe(coupleId)
        .catch { Timber.w(it, "Capsules unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun seal(me: UserProfile, title: String, text: String, openOn: LocalDate) {
        viewModelScope.launch {
            runCatching { app.capsuleRepository.add(coupleId, me, title, text, openOn) }
                .onSuccess {
                    CapsuleDraft.clear(app)
                    Telemetry.track(AnalyticsEvent.CapsuleSealed(ChronoUnit.DAYS.between(LocalDate.now(), openOn).toInt()))
                }
                .onFailure { Timber.w(it, "Sealing the letter failed") }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { runCatching { app.capsuleRepository.delete(coupleId, id) }.onFailure { Timber.w(it, "Deleting the letter failed") } }
    }
}

/** Zaman kapsülü (approved design 19): letters sealed until a day in the future. */
@Composable
fun CapsuleScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit, onOpenedList: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: CapsuleViewModel = viewModel(key = "capsule-$coupleId") { CapsuleViewModel(app, coupleId) }
    val capsules by vm.capsules.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = LocalDate.now()
    var writing by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Capsule?>(null) }

    // "Emin misin?" before deleting.
    var askDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    askDelete?.let { act ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.del_letter_title),
            body = stringResource(R.string.del_letter_body),
            confirm = stringResource(R.string.del_confirm),
            onConfirm = { act(); askDelete = null },
            onDismiss = { askDelete = null },
        )
    }

    val all = capsules.orEmpty()
    val sealed = all.filter { !it.isOpen(today) }.sortedBy { it.openOn }
    val opened = all.filter { it.isOpen(today) }.sortedByDescending { it.openOn }
    // Hero: the partner's nearest sealed letter, else my own nearest.
    val hero = sealed.firstOrNull { it.by != me.uid } ?: sealed.firstOrNull()
    val rest = sealed.filter { it.id != hero?.id } + opened

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CenteredTopBar(stringResource(R.string.tc_title), onBack) {
            RoundSvgButton(CLOCK_D, stringResource(R.string.tc_opened_list), onOpenedList)
        }

        // Hero envelope.
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(shape, 12.dp)
                .clip(shape)
                .background(PeachSoft)
                .then(if (hero != null) Modifier.bounceClick(pressedScale = 0.98f) { viewing = hero } else Modifier)
                .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Envelope()
            if (hero != null) {
                val days = ChronoUnit.DAYS.between(today, hero.openOn).toInt()
                Text(
                    if (hero.by == me.uid) stringResource(R.string.tc_from_me)
                    else stringResource(R.string.tc_from_partner, TurkishSuffix.ablative(hero.byName.ifBlank { partnerName })),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Kit.CellLabel,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
                )
                Text(stringResource(R.string.tc_days, days), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp, color = Brown)
                Text(stringResource(R.string.tc_opens_in), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                Row(
                    Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SvgStrokeIcon(Kit.LOCK_D, Brown, Modifier.size(16.dp))
                    Text(
                        listOf(hero.openOn.format(DATE), hero.title).filter { it.isNotBlank() }.joinToString(" · "),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(stringResource(R.string.tc_none_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.tc_none_body), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Kit.CellLabel, textAlign = TextAlign.Center)
            }
        }

        if (rest.isNotEmpty()) {
            Text(stringResource(R.string.tc_letters), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.padding(start = 4.dp))
            rest.forEach { c -> LetterRow(c, me, partnerName, today) { viewing = c } }
        }

        MainAction(stringResource(R.string.tc_write), Kit.PENCIL_D, { writing = true })
    }

    if (writing) {
        WriteSheet(onDismiss = { writing = false }) { title, text, date ->
            writing = false
            vm.seal(me, title, text, date)
        }
    }
    viewing?.let { c ->
        LetterSheet(
            c, today, mine = c.by == me.uid,
            onDismiss = { viewing = null },
            onDelete = { askDelete = { vm.delete(c.id); viewing = null } },
        )
    }
}

@Composable
private fun LetterRow(c: Capsule, me: UserProfile, partnerName: String, today: LocalDate, onClick: () -> Unit) {
    val open = c.isOpen(today)
    val who = if (c.by == me.uid) stringResource(R.string.tc_by_me) else stringResource(R.string.tc_by, c.byName.ifBlank { partnerName })
    val date = TurkishSuffix.locative(c.openOn.format(DATE))
    WhiteCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(if (open) PeachSoft else Kit.MintCell), contentAlignment = Alignment.Center) {
                SvgStrokeIcon(if (open) Kit.ENVELOPE_D else Kit.LOCK_D, Brown, Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(c.title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(if (open) R.string.tc_opened_on else R.string.tc_opens_on, who, date),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (open) TagChip(stringResource(R.string.tc_read), PeachSoft)
            else TagChip(stringResource(R.string.tc_days, ChronoUnit.DAYS.between(today, c.openOn).toInt()), Kit.MintCell)
        }
    }
}

/** The design's envelope with a heart seal (150×112), gently bobbing. */
@Composable
private fun Envelope() {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        0f, -4f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
    )
    val heart = remember { svgPath(HEART_D) }
    val flap = remember { svgPath("M8 20l67 46 67-46") }
    Canvas(Modifier.size(150.dp, 112.dp).graphicsLayer { translationY = bob.dp.toPx() }) {
        val s = size.width / 150f
        scale(s, pivot = Offset.Zero) {
            drawRoundRect(CreamCard, Offset(5f, 12f), Size(140f, 94f), CornerRadius(16f))
            drawRoundRect(Brown, Offset(5f, 12f), Size(140f, 94f), CornerRadius(16f), style = Stroke(3f))
            drawPath(flap, Brown, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(Peach, 19f, Offset(75f, 66f))
            drawCircle(Brown, 19f, Offset(75f, 66f), style = Stroke(3f))
            translate(63f, 55f) { drawPath(heart, Brown) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteSheet(onDismiss: () -> Unit, onSeal: (String, String, LocalDate) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // The letter is a draft on the phone until it is sealed: closing the sheet (or Back) never loses it.
    val draft = remember { CapsuleDraft.load(context) }
    var title by rememberSaveable { mutableStateOf(draft.first) }
    var text by rememberSaveable { mutableStateOf(draft.second) }
    var dateText by rememberSaveable {
        mutableStateOf(draft.third?.takeIf { runCatching { LocalDate.parse(it).isAfter(LocalDate.now()) }.getOrDefault(false) } ?: LocalDate.now().plusMonths(3).toString())
    }
    androidx.compose.runtime.LaunchedEffect(title, text, dateText) { CapsuleDraft.save(context, title, text, dateText) }
    val keptText = stringResource(R.string.tc_draft_kept)
    val closeKeepingDraft: () -> Unit = {
        if (title.isNotBlank() || text.isNotBlank()) android.widget.Toast.makeText(context, keptText, android.widget.Toast.LENGTH_SHORT).show()
        onDismiss()
    }
    var picking by remember { mutableStateOf(false) }
    val date = LocalDate.parse(dateText)
    ModalBottomSheet(onDismissRequest = closeKeepingDraft, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.tc_editor_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
            SheetField(title, { title = it.take(CapsuleRepository.MAX_TITLE) }, stringResource(R.string.tc_editor_name))
            SheetField(text, { text = it.take(CapsuleRepository.MAX_TEXT) }, stringResource(R.string.tc_editor_text), singleLine = false, minHeight = 160.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp)).background(CreamCard)
                    .bounceClick { picking = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SvgStrokeIcon(Kit.LOCK_D, Brown, Modifier.size(18.dp))
                Text(stringResource(R.string.tc_editor_date, date.format(DATE)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            }
            MainAction(stringResource(R.string.tc_seal), Kit.LOCK_D, { onSeal(title.trim(), text, date) }, enabled = title.isNotBlank() && text.isNotBlank())
        }
    }
    if (picking) {
        LovelyyDatePicker(
            initial = date, title = stringResource(R.string.tc_editor_pick), allowFuture = true, onlyFuture = true,
            onDismiss = { picking = false },
            onPick = { dateText = it.toString(); picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterSheet(c: Capsule, today: LocalDate, mine: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val open = c.isOpen(today)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val heading = when {
                open -> c.title
                mine -> stringResource(R.string.tc_from_me)
                else -> stringResource(R.string.tc_from_partner, c.byName.ifBlank { stringResource(R.string.home_partner_fallback) })
            }
            Text(heading, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
            if (open) {
                WhiteCard(padding = 18.dp) {
                    Text(c.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, color = Brown)
                }
                Text(c.byName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrownSoft, modifier = Modifier.align(Alignment.End))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SvgStrokeIcon(Kit.LOCK_D, BrownSoft, Modifier.size(18.dp))
                    Text(stringResource(R.string.tc_sealed_until, c.openOn.format(DATE)), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft)
                }
            }
            if (mine) {
                Text(
                    stringResource(R.string.tc_delete), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFA8401C),
                    modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick(onClick = onDelete).padding(12.dp),
                )
            }
        }
    }
}
