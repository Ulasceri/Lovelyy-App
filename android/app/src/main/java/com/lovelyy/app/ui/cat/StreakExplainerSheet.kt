package com.lovelyy.app.ui.cat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.StreakExplainer
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.drawSvg
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownMuted
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.FlameInk
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.SheetHandle
import kotlinx.coroutines.launch

/** The design's streak flame (24×24), filled peach with a deep peach outline. */
internal const val FLAME_D = "M12 21c-4 0-6-2.7-6-6 0-4 4-6 4-10 3 2 4 4 4 6 1-1 1.5-2 1.5-3 2 2 2.5 4.5 2.5 7 0 3.3-2 6-6 6z"
private const val PEOPLE_D = "M12.5 8a3.5 3.5 0 1 1-7 0a3.5 3.5 0 1 1 7 0z M19.5 9a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0z M3 20a6 6 0 0 1 12 0M15 20a4 4 0 0 1 6-3.5"
private const val MOON_D = "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"
private const val SPARK_D = "M12 2l2.6 7.4L22 12l-7.4 2.6L12 22l-2.6-7.4L2 12l7.4-2.6z"

/** The streak flame as on the boards: peach fill, [FlameInk] outline. */
@Composable
internal fun StreakFlame(size: Dp, modifier: Modifier = Modifier) {
    val flame = remember { svgPath(FLAME_D) }
    Canvas(modifier.size(size)) {
        drawSvg(flame, 24f, Peach)
        drawSvg(flame, 24f, FlameInk, strokeWidth = 1.6f)
    }
}

/**
 * "Seri nedir?" (approved board S1): what counts as a streak day, what the streak is and what happens when a day
 * is missed. Opens by itself once when the couple's streak first reaches 1 (see [CatViewModel]) and again from the
 * Yuva streak chip and the Kedi bakımı streak card.
 *
 * [showPlusNote]: the board's last line about Plus (days off, make-up within 24 h). It is information only and
 * links nowhere until those features exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StreakExplainerSheet(
    catName: String,
    myName: String,
    partnerName: String,
    streak: Int,
    bothCaredToday: Boolean,
    onDismiss: () -> Unit,
    showPlusNote: Boolean = true,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val catTo = TurkishSuffix.dative(catName)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Cream,
        scrimColor = Brown.copy(alpha = 0.45f),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(SheetHandle))
        },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: the two of you and the flame, then the day.
            Row(
                Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeaderAvatars(myName, partnerName)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val title = when (StreakExplainer.heading(streak)) {
                        StreakExplainer.Heading.Intro -> stringResource(R.string.streak_sheet_title_intro)
                        StreakExplainer.Heading.Started -> stringResource(R.string.streak_sheet_title_started)
                        StreakExplainer.Heading.Running -> stringResource(R.string.streak_sheet_title_running, streak)
                    }
                    Text(
                        title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = Brown,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (bothCaredToday) {
                        Text(
                            stringResource(R.string.streak_sheet_both_today, catTo),
                            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = BrownSoft,
                        )
                    }
                }
            }

            val cardShape = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().softShadow(cardShape, 6.dp).clip(cardShape).background(CreamCard).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ExplainRow(
                    icon = { SvgStrokeIcon(PEOPLE_D, Brown, Modifier.size(20.dp)) },
                    tint = PeachTint,
                    title = stringResource(R.string.streak_sheet_count_title),
                    body = stringResource(R.string.streak_sheet_count_body, catTo),
                )
                ExplainRow(
                    icon = { StreakFlame(20.dp) },
                    tint = PeachTint,
                    title = stringResource(R.string.streak_sheet_meaning_title),
                    body = stringResource(R.string.streak_sheet_meaning_body),
                )
                ExplainRow(
                    icon = { SvgStrokeIcon(MOON_D, Brown, Modifier.size(20.dp)) },
                    tint = MintTint,
                    title = stringResource(R.string.streak_sheet_missed_title),
                    body = stringResource(R.string.streak_sheet_missed_body),
                )
            }

            if (showPlusNote) {
                // Information only: no tap target until Plus days off / make-up exist.
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PeachSoft).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Brown).padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SvgIcon(SPARK_D, Cream, Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(R.string.streak_sheet_plus_badge),
                            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Cream, maxLines = 1,
                        )
                    }
                    Text(
                        stringResource(R.string.streak_sheet_plus_body),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            PeachButton(
                stringResource(R.string.streak_sheet_ok),
                onClick = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Me (peach), my partner (mint) and a white flame circle, overlapping like the board. */
@Composable
private fun HeaderAvatars(myName: String, partnerName: String) {
    val size = 52.dp
    val overlap = 16.dp
    Box(Modifier.size(width = size * 3 - overlap * 2, height = size)) {
        InitialAvatar(myName, Peach, size, 20.sp)
        InitialAvatar(partnerName, Mint, size, 20.sp, Modifier.offset(x = size - overlap))
        Box(
            Modifier
                .offset(x = (size - overlap) * 2)
                .size(size)
                .softShadow(CircleShape, 4.dp)
                .clip(CircleShape)
                .background(Cream)
                .padding(3.dp)
                .clip(CircleShape)
                .background(CreamCard),
            contentAlignment = Alignment.Center,
        ) { StreakFlame(26.dp) }
    }
}

@Composable
private fun ExplainRow(icon: @Composable () -> Unit, tint: Color, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            Text(body, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownSoft)
        }
    }
}
