package com.lovelyy.app.ui.plus

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.PlusLimits
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownMuted
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.PeachDisabled
import com.lovelyy.app.ui.theme.Quicksand

/**
 * A list's free limit as one screen sees it. [blocked]: free and at (or over) the limit, so adding opens the
 * plans instead (PlusLimits.canAdd). Outside Home ([LocalPlus] is null) nothing is limited.
 */
class LimitGate(val limit: PlusLimits.Limit, val count: Int, private val access: PlusAccess?) {
    val blocked: Boolean get() = access != null && PlusLimits.atLimit(limit, count, access.isPlus)

    /** Over the limit: the P9 note shows on top of the list. */
    val over: Boolean get() = access != null && PlusLimits.overLimit(limit, count, access.isPlus)

    /** Runs [add] when adding is allowed, else opens the plans (paywall source "lock_<limit>"). */
    fun tryAdd(add: () -> Unit) {
        if (blocked) access?.openPlans?.invoke(PaywallSource.limit(limit)) else add()
    }

    fun openPlans() {
        access?.openPlans?.invoke(PaywallSource.limit(limit))
    }
}

@Composable
fun rememberLimitGate(limit: PlusLimits.Limit, count: Int): LimitGate = LimitGate(limit, count, LocalPlus.current)

/** Strings of one limit (board P9 copy, per list). */
private data class LimitTexts(@StringRes val title: Int, @PluralsRes val body: Int, @PluralsRes val count: Int)

private fun texts(limit: PlusLimits.Limit): LimitTexts = when (limit) {
    PlusLimits.Limit.Notes -> LimitTexts(R.string.limit_notes_title, R.plurals.limit_notes_body, R.plurals.limit_notes_count)
    PlusLimits.Limit.Events -> LimitTexts(R.string.limit_events_title, R.plurals.limit_events_body, R.plurals.limit_events_count)
    PlusLimits.Limit.Dreams -> LimitTexts(R.string.limit_dreams_title, R.plurals.limit_dreams_body, R.plurals.limit_dreams_count)
    PlusLimits.Limit.Capsules -> LimitTexts(R.string.limit_capsules_title, R.plurals.limit_capsules_body, R.plurals.limit_capsules_count)
    PlusLimits.Limit.Wheels -> LimitTexts(R.string.limit_wheels_title, R.plurals.limit_wheels_body, R.plurals.limit_wheels_count)
    PlusLimits.Limit.Drawings -> LimitTexts(R.string.limit_drawings_title, R.plurals.limit_drawings_body, R.plurals.limit_drawings_count)
}

/**
 * P9 "sınır dolu, yalnızca okunur" (board PlusSaltOkunur): the mint note at the top of a list that is over its free
 * limit (usually after Plus ended). Everything below stays readable, editable and deletable; only adding goes
 * through Plus. Shows nothing otherwise.
 */
@Composable
fun PlusLimitBanner(gate: LimitGate, modifier: Modifier = Modifier) {
    if (!gate.over) return
    val t = texts(gate.limit)
    val max = gate.limit.free
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MintTint).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(CreamCard), contentAlignment = Alignment.Center) {
                SvgStrokeIcon(Kit.LOCK_D, MintInk, Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(t.title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp, color = MintInk)
                Text(
                    pluralStringResource(t.body, gate.count, gate.count, max),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = MintInk,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(CreamCard)
                    .semantics { role = Role.Button }
                    .bounceClick(onClick = gate::openPlans)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.limit_see_plus), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, maxLines = 1)
            }
            Text(
                pluralStringResource(t.count, gate.count, gate.count, max),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MintInk,
            )
        }
    }
}

/**
 * The P9 add button while adding is blocked: pale, with a lock ("Yeni not · Plus ile"). It still reacts, and opens
 * the plans. Same size as the screen's normal main action (56dp, fully rounded).
 */
@Composable
fun LockedAddButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(PeachDisabled)
            .semantics { role = Role.Button }
            .bounceClick(pressedScale = 0.97f, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgStrokeIcon(Kit.LOCK_D, BrownMuted, Modifier.size(18.dp), strokeWidth = 2.2f)
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrownMuted, modifier = Modifier.padding(start = 10.dp))
    }
}
