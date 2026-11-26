package com.lovelyy.app.ui.plus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.data.DEFAULT_CAT_NAME
import com.lovelyy.app.text.TurkishSuffix
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
 * P1 "Deneme hediyesi" (approved board PlusDeneme): right after "Eşleştiniz", before the permission primers, once.
 * No price and no payment; the one button starts the couple's 14-day trial and moves on. No names (onboarding
 * rule); the cat is new, so it still has its default name.
 */
@Composable
fun PlusTrialGiftScreen(onStart: () -> Unit) {
    val cat = DEFAULT_CAT_NAME
    Column(
        Modifier.fillMaxSize().background(Cream).statusBarsPadding().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp))
            Box(Modifier.size(168.dp)) {
                Image(
                    painterResource(R.drawable.cat_base),
                    contentDescription = cat,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(168.dp)
                        .softShadow(CircleShape, 12.dp)
                        .clip(CircleShape)
                        .background(CatBg)
                        .border(5.dp, CreamCard, CircleShape),
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = (-2).dp).size(60.dp).clip(CircleShape).background(Peach).border(4.dp, Cream, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { SvgStrokeIcon(PlusIcons.GIFT, Brown, Modifier.size(28.dp)) }
                SvgIcon(PlusIcons.SPARKLE, Peach, Modifier.align(Alignment.TopStart).offset(x = (-22).dp, y = 18.dp).size(22.dp))
                SvgIcon(PlusIcons.SPARKLE, Mint, Modifier.align(Alignment.TopEnd).offset(x = 26.dp, y = 46.dp).size(14.dp))
                SvgIcon(PlusIcons.SPARKLE, Mint, Modifier.align(Alignment.BottomStart).offset(x = (-8).dp, y = (-26).dp).size(12.dp))
            }
            Spacer(Modifier.height(24.dp))
            PlusPill(stringResource(R.string.plus_name))
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.plus_trial_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp,
                color = Brown, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.plus_trial_body), fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp,
                color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 310.dp),
            )
            SoftCard(Modifier.fillMaxWidth().padding(top = 22.dp), padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BenefitRow(PlusIcons.SHIELD, PeachTint, stringResource(R.string.plus_b_streak), stringResource(R.string.plus_b_streak_sub))
                    BenefitRow(PlusIcons.SOFA, MintTint, stringResource(R.string.plus_b_things, TurkishSuffix.genitive(cat)), stringResource(R.string.plus_b_things_sub))
                    BenefitRow(PlusIcons.GRID, PeachTint, stringResource(R.string.plus_b_widgets), stringResource(R.string.plus_b_widgets_sub, cat))
                    BenefitRow(PlusIcons.BUBBLE, MintTint, stringResource(R.string.plus_b_question), stringResource(R.string.plus_b_question_sub))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        PeachButton(stringResource(R.string.plus_trial_start), onStart)
    }
}
