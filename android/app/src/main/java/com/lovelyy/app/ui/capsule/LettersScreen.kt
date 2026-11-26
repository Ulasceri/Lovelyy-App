package com.lovelyy.app.ui.capsule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.Capsule
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.history.DAY_MONTH_YEAR
import com.lovelyy.app.ui.history.EmptyNote
import com.lovelyy.app.ui.history.HistoryRow
import com.lovelyy.app.ui.history.InnerColumn
import com.lovelyy.app.ui.history.MiniAvatar
import com.lovelyy.app.ui.history.Muted
import com.lovelyy.app.ui.history.OutChip
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.history.localDate
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.catch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val MintCard = Color(0xFFE6F5F0)
private val ChipGrey = Color(0xFFF6EEE8)
private val DashLine = Color(0xFFF0E2D8)

/**
 * Açılmış mektuplar (approved boards 30-31): the time-capsule letters whose day has come, grouped by year,
 * and one letter to read. Sealed letters never show their text here; only how many are still waiting.
 * [startId] opens one letter straight away (from Arama).
 */
@Composable
fun LettersScreen(me: UserProfile, partner: UserProfile?, startId: String?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val capsules by remember(coupleId) { app.capsuleRepository.observe(coupleId).catch { emit(emptyList()) } }
        .collectAsStateWithLifecycle(null)
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = LocalDate.now()
    val all = capsules.orEmpty()
    val opened = all.filter { it.isOpen(today) }.sortedByDescending { it.openOn }
    val sealed = all.filter { !it.isOpen(today) }.sortedBy { it.openOn }

    var openId by rememberSaveable { mutableStateOf(startId) }
    val reading = opened.firstOrNull { it.id == openId }
    // Opened straight from search: back leaves the screen; opened from the list: back returns to the list.
    BackHandler(enabled = openId != null && openId != startId) { openId = null }

    if (reading != null) {
        LetterRead(reading, me, partnerName, onBack = { if (openId == startId) onBack() else openId = null })
        return
    }

    InnerColumn(spacing = 10) {
        CenteredTopBar(stringResource(R.string.ol_title), onBack)
        if (capsules != null) {
            Text(
                if (opened.isEmpty()) stringResource(R.string.ol_none) else stringResource(R.string.ol_intro, opened.size),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, color = SubText,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        opened.groupBy { it.openOn.year }.forEach { (year, letters) ->
            Text(year.toString(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            letters.forEach { c -> LetterRow(c, me, partnerName) { openId = c.id } }
        }
        if (capsules != null && opened.isEmpty() && sealed.isEmpty()) EmptyNote(stringResource(R.string.ol_empty))
        sealed.firstOrNull()?.let { next ->
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(22.dp)).background(MintCard).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    SvgStrokeIcon(Kit.LOCK_D, Brown, Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ol_waiting, sealed.size), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
                    Text(
                        stringResource(R.string.ol_nearest, ChronoUnit.DAYS.between(today, next.openOn)),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubText,
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterRow(c: Capsule, me: UserProfile, partnerName: String, onClick: () -> Unit) {
    val who = if (c.by == me.uid) stringResource(R.string.se_by_me) else stringResource(R.string.se_by, c.byName.ifBlank { partnerName })
    HistoryRow(
        icon = { SvgStrokeIcon(Kit.ENVELOPE_D, Brown, Modifier.size(22.dp)) },
        iconBg = OutChip,
        title = c.title,
        subtitle = who + " · " + stringResource(R.string.ol_opened_on, TurkishSuffix.locative(c.openOn.format(DAY_MONTH_YEAR))),
        onClick = onClick,
        extra = {
            Text(
                "“" + c.text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim() + "”",
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubText, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Text(
                stringResource(R.string.ol_read), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                modifier = Modifier.clip(CircleShape).background(OutChip).padding(horizontal = 12.dp, vertical = 7.dp),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterRead(c: Capsule, me: UserProfile, partnerName: String, onBack: () -> Unit) {
    val mine = c.by == me.uid
    val author = if (mine) me.displayName else c.byName.ifBlank { partnerName }
    val written = c.at.localDate()
    val sealedDays = ChronoUnit.DAYS.between(written, c.openOn).coerceAtLeast(0)
    InnerColumn(spacing = 12) {
        CenteredTopBar(c.title, onBack)
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 10.dp).clip(shape).background(Color.White)
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniAvatar(author, if (mine) Peach else Mint, 40)
                Column {
                    Text(
                        if (mine) stringResource(R.string.ol_from_me, TurkishSuffix.dative(partnerName)) else stringResource(R.string.ol_from, TurkishSuffix.ablative(author)),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                    )
                    Text(
                        stringResource(R.string.ol_written_on, TurkishSuffix.locative(written.format(DAY_MONTH_YEAR))),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.clip(CircleShape).background(OutChip).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SvgStrokeIcon(Kit.ENVELOPE_D, Brown, Modifier.size(14.dp))
                    Text(
                        stringResource(R.string.ol_opened_on, TurkishSuffix.locative(c.openOn.format(DAY_MONTH_YEAR))),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
                    )
                }
                if (sealedDays > 0) {
                    Text(
                        stringResource(R.string.ol_sealed_days, sealedDays), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
                        modifier = Modifier.clip(CircleShape).background(ChipGrey).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().height(2.dp).drawBehind {
                    drawLine(DashLine, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                },
            )
            Text(c.text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 25.sp, color = Brown)
            Text(author, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.align(Alignment.End))
        }
    }
}
