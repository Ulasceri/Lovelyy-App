package com.lovelyy.app.ui.plus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusSource
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand

/**
 * P6b "Plus açıldı" (board PlusHediyeAcildi): shown once to the partner who did not buy, the first time they
 * open the app after a gift (with the note in a bubble) or after the other partner subscribed (no note, "Plus
 * açıldı, ikinizde de"). Full screen, no bottom menu.
 */
@Composable
fun PlusCelebration(plus: PlusInfo, buyerName: String, catName: String, onHeart: () -> Unit, onClose: () -> Unit) {
    val gift = plus.source == PlusSource.Gift
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }

    Column(
        Modifier.fillMaxSize().background(Cream).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Box(Modifier.size(150.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            Image(
                painterResource(R.drawable.cat_base), catName, contentScale = ContentScale.Crop,
                modifier = Modifier.size(150.dp).softShadow(CircleShape, 12.dp).clip(CircleShape).background(CatBg).border(5.dp, CreamCard, CircleShape),
            )
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 6.dp).size(54.dp).clip(CircleShape).background(Peach).border(4.dp, Cream, CircleShape),
                contentAlignment = Alignment.Center,
            ) { SvgStrokeIcon(if (gift) PlusIcons.GIFT else PlusIcons.SPARKLE, Brown, Modifier.size(24.dp)) }
            SvgIcon(PlusIcons.SPARKLE, Peach, Modifier.align(Alignment.TopStart).offset(x = (-20).dp, y = 14.dp).size(20.dp))
            SvgIcon(PlusIcons.SPARKLE, Mint, Modifier.align(Alignment.TopEnd).offset(x = 22.dp, y = 40.dp).size(13.dp))
        }
        Spacer(Modifier.height(22.dp))
        PlusPill(stringResource(R.string.plus_cele_badge))
        Text(
            if (gift) stringResource(R.string.plus_cele_gift_title, buyerName) else stringResource(R.string.plus_cele_sub_title),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = Brown, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            if (gift) stringResource(R.string.plus_cele_gift_sub) else stringResource(R.string.plus_cele_sub_sub, buyerName),
            fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        val note = plus.giftNote
        if (gift && !note.isNullOrBlank()) {
            SoftCard(Modifier.fillMaxWidth().padding(top = 18.dp), padding = 16.dp) {
                Text(
                    stringResource(R.string.ol_quote, note), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp, color = Brown,
                )
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InitialAvatar(buyerName, Peach, 28.dp, 12.sp, border = false)
                    Text(buyerName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            Chip(PlusIcons.SHIELD, stringResource(R.string.plus_b_streak), PeachTint)
            Chip(PlusIcons.SOFA, stringResource(R.string.plus_b_things, TurkishSuffix.genitive(catName)), MintTint)
            Chip(PlusIcons.GRID, stringResource(R.string.plus_cele_widgets), PeachTint)
        }
        Spacer(Modifier.weight(1f))
        PeachButton(stringResource(R.string.plus_cele_heart, TurkishSuffix.dative(buyerName)), onHeart)
        GhostButton(stringResource(R.string.action_close), onClose, Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Chip(icon: String, text: String, bg: androidx.compose.ui.graphics.Color) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, color = Brown, textAlign = TextAlign.Center, maxLines = 2)
    }
}
