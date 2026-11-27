package com.lovelyy.app.ui.plus

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand

/**
 * P6a "Sevgiline Plus hediye et" (board PlusHediye): one product, 1 year of Plus, a single payment that never
 * renews, with an optional short note the partner sees once in the celebration (P6b). Opened from P8.
 */
@Composable
fun PlusGiftScreen(vm: PlusViewModel, partnerName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val prices = plusPrices(catalog)
    var note by rememberSaveable { mutableStateOf("") }

    // navigationBarsPadding + 140dp keep the footnote clear of the bottom menu and its heart (like Kedi bakımı).
    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        CenteredTopBar(stringResource(R.string.plus_gift_top), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.plus_gift_title, TurkishSuffix.dative(partnerName)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = Brown)
                Text(stringResource(R.string.plus_gift_sub), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
            }
            // The one product.
            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(PeachSoft).border(2.5.dp, PeachDeep, shape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                        SvgStrokeIcon(PlusIcons.GIFT, Brown, Modifier.size(22.dp))
                    }
                    Text(stringResource(R.string.plus_gift_product), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(prices.gift, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
                        Text(stringResource(R.string.plus_one_time), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Kit.CellLabel)
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SvgStrokeIcon(Kit.CHECK_D, Kit.Green, Modifier.padding(top = 2.dp).size(18.dp), strokeWidth = 2.4f)
                    Column {
                        Text(stringResource(R.string.plus_gift_no_renew), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown)
                        Text(stringResource(R.string.plus_gift_no_renew_sub), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = Kit.CellLabel)
                    }
                }
            }
            // Optional note.
            SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.plus_gift_note), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
                    Text(" " + stringResource(R.string.plus_gift_note_optional), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.plus_gift_note_count, note.length, PlusRules.GIFT_NOTE_MAX),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                    )
                }
                val fieldShape = RoundedCornerShape(18.dp)
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(PlusRules.GIFT_NOTE_MAX) },
                    textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown),
                    cursorBrush = SolidColor(PeachDeep),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(fieldShape).background(CreamCard).border(1.5.dp, Outline, fieldShape).padding(14.dp),
                        ) {
                            if (note.isEmpty()) Text(stringResource(R.string.plus_gift_note_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft)
                            inner()
                        }
                    },
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MintTint).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InitialAvatar(partnerName, Mint, 32.dp, 13.sp, border = false)
                Text(
                    stringResource(R.string.plus_gift_partner_sees, partnerName),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = Brown, modifier = Modifier.weight(1f),
                )
            }
        }
        PeachButton(
            stringResource(R.string.plus_gift_buy, prices.gift),
            onClick = { vm.buy(context.findActivity(), PlusPlan.Gift, note) },
            loading = busy,
        )
        Text(
            stringResource(R.string.plus_gift_disc), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp,
            color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 140.dp),
        )
        Spacer(Modifier.height(0.dp))
    }
}
