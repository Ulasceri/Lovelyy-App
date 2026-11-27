package com.lovelyy.app.ui.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.PlusSource
import com.lovelyy.app.data.PlusStatus
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.home.formatDate
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand

/**
 * P2a: the Plus card on the Biz tab, under the couple card. During the trial it shows the days left and
 * progress (board PlusBizKart); afterwards the plan and its state, or an invitation when Plus is off.
 * Tapping opens Plus management (P8).
 */
@Composable
fun BizPlusCard(status: PlusStatus, onClick: (() -> Unit)?, modifier: Modifier = Modifier, showMore: Boolean = true) {
    SoftCard(modifier.fillMaxWidth(), padding = 16.dp, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (status) {
                is PlusStatus.Trial -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlusPill(stringResource(R.string.plus_card_trial_pill), fontSize = 12)
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.plus_card_trial_day, status.day), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pluralStringResource(R.plurals.plus_days_left, status.daysLeft, status.daysLeft), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown)
                        Text(
                            stringResource(R.string.plus_until, TurkishSuffix.dative(shortDate(status.endsAt))),
                            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    val progress = status.day / 14f
                    val cd = stringResource(R.string.plus_card_progress_cd, status.day)
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Sand).semantics { contentDescription = cd }) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Peach))
                    }
                    MoreRow(stringResource(R.string.plus_card_trial_note), if (showMore) stringResource(R.string.plus_card_more) else null)
                }
                is PlusStatus.Paid -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlusPill(stringResource(planLabel(status.plan)), fontSize = 12)
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(if (status.autoRenew) R.string.plus_state_active else R.string.plus_state_ending),
                            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (status.autoRenew) Kit.Green else BrownSoft,
                        )
                    }
                    Text(
                        stringResource(
                            if (status.autoRenew) R.string.plus_renews_on else R.string.plus_ends_on,
                            TurkishSuffix.locative(formatDate(status.until)),
                        ),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown,
                    )
                    MoreRow(stringResource(R.string.plus_card_paid_note), if (showMore) stringResource(R.string.plus_card_more) else null)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlusPill(stringResource(R.string.plus_pill), fontSize = 12)
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.plus_state_off), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                    }
                    Text(stringResource(R.string.plus_card_free_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                    MoreRow(stringResource(R.string.plus_card_free_sub), if (showMore) stringResource(R.string.plus_card_see_plans) else null)
                }
            }
        }
    }
}

@Composable
private fun MoreRow(note: String, action: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(note, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownSoft, modifier = Modifier.weight(1f))
        if (action != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(action, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
            SvgStrokeIcon(PlusIcons.CHEVRON, Brown, Modifier.size(16.dp))
        }
    }
}

/**
 * P2b: the gentle peach card under the cat's care buttons (board PlusGun12). Used on trial days 12-13 and, with
 * other words, for the takeover hint (P10's "small card on Home first"). Closing it hides it for the day.
 */
@Composable
fun HomePlusCard(title: String, body: String, action: String, onAction: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier, footnote: String? = null) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PeachSoft).padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                Text(body, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = Kit.CellLabel)
            }
            val closeLabel = stringResource(R.string.plus_card_close)
            Box(
                Modifier.offset(x = 4.dp, y = (-8).dp).size(44.dp).clip(CircleShape)
                    .semantics { contentDescription = closeLabel; role = Role.Button }
                    .bounceClick(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { SvgStrokeIcon(Kit.X_D, Kit.CellLabel, Modifier.size(18.dp), strokeWidth = 2.2f) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(Peach).bounceClick(onClick = onAction).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(action, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1) }
            if (footnote != null) Text(footnote, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Kit.CellLabel)
        }
    }
}

/** True when the paid Plus came as a gift (P8/P6b wording). */
internal val PlusStatus.Paid.isGift: Boolean get() = source == PlusSource.Gift
