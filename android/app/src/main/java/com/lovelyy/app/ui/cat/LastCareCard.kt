package com.lovelyy.app.ui.cat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.CareRewards
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CoupleClock
import com.lovelyy.app.data.DisplayTime
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.drawSvg
import com.lovelyy.app.ui.components.svgPath
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatStripStart
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand

/**
 * P7b "Son bakım": each partner's latest care today, under the day counter on Home (it replaced the grey status
 * line under the care buttons). Partner on the left, me on the right, as on the board; each with an avatar, a small
 * badge of what they did, the action and how long ago. Nobody cared yet: "Bora henüz bakmadı".
 */
@Composable
fun LastCareCard(me: UserProfile, partner: UserProfile?, modifier: Modifier = Modifier) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val zone by CoupleClock.zone.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val myName = me.displayName.ifBlank { stringResource(R.string.cat_you) }
    val recent = ui?.doc?.recent ?: return
    val latest = CareRewards.todayLatest(recent, me.uid, streakToday(), zone)
    val mine = latest.firstOrNull { it.uid == me.uid }
    val theirs = latest.firstOrNull { it.uid != me.uid }

    val shape = RoundedCornerShape(24.dp)
    val label = stringResource(R.string.last_care_label)
    val partnerLine = theirs?.let { todayText(it, me.uid, partnerName) } ?: stringResource(R.string.cc_partner_none, partnerName)
    val myLine = mine?.let { todayText(it, me.uid, partnerName) } ?: stringResource(R.string.cc_me_none)
    val partnerAgo = theirs?.let { ago(it.at) }
    val myAgo = mine?.let { ago(it.at) }
    Row(
        modifier
            .fillMaxWidth()
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(CreamCard)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(label, partnerLine, partnerAgo, myLine, myAgo).joinToString(", ")
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CareHalf(partnerName, Mint, theirs, partnerLine, partnerAgo, Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(30.dp).background(CatStripStart))
        CareHalf(myName, Peach, mine, myLine, myAgo, Modifier.weight(1f))
    }
}

@Composable
private fun CareHalf(name: String, color: Color, action: CatAction?, line: String, ago: String?, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp)) {
            InitialAvatar(name, color, 36.dp, 15.sp, border = false)
            action?.let { a ->
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(CreamCard)
                        .border(1.5.dp, CatStripStart, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { BadgeIcon(a.type.badgeIcon, color, Modifier.size(11.dp)) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                line, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, color = Brown,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (ago != null) {
                Text(ago, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft, maxLines = 1)
            }
        }
    }
}

/** The badge's icon: filled with the person's color, outlined in brown (board P7b). */
@Composable
private fun BadgeIcon(d: String, fill: Color, modifier: Modifier) {
    val path = remember(d) { svgPath(d) }
    Canvas(modifier) {
        drawSvg(path, 24f, fill)
        drawSvg(path, 24f, Brown, 2.4f)
    }
}

/** "az önce", "12 dakika önce", "2 saat önce" (only today's care is shown, so never days). */
@Composable
private fun ago(at: Long): String {
    val minutes = DisplayTime.minutesAgo(at, System.currentTimeMillis())
    return when {
        minutes < 1 -> stringResource(R.string.time_ago_now)
        minutes < 60 -> pluralStringResource(R.plurals.time_ago_minutes, minutes.toInt(), minutes.toInt())
        else -> (minutes / 60).toInt().let { pluralStringResource(R.plurals.time_ago_hours, it, it) }
    }
}

private const val BOWL_ICON = "M3 12h18a9 6 0 0 1-18 0z"
private const val DROP_ICON = "M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"
private const val BALL_ICON = "M12 4a8 8 0 1 1 0 16a8 8 0 1 1 0-16z"
private const val MOON_ICON = "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"
private const val SUN_ICON = "M12 7a5 5 0 1 1 0 10a5 5 0 1 1 0-10z"

/** The small badge on the avatar: what that person did last. */
private val CatActionType.badgeIcon: String
    get() = when (this) {
        CatActionType.Feed -> BOWL_ICON
        CatActionType.Water -> DROP_ICON
        CatActionType.Play -> BALL_ICON
        CatActionType.Pet, CatActionType.Name -> HEART_D
        CatActionType.Sleep -> MOON_ICON
        CatActionType.Wake -> SUN_ICON
    }
