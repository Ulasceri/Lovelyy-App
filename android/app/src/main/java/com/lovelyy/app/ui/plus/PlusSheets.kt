package com.lovelyy.app.ui.plus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.CatLevels
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.home.formatDate
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.NeedLow
import com.lovelyy.app.ui.theme.NeedOk
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand
import kotlinx.coroutines.launch

/** Data the lock preview shows (the couple's own cat, like the real widget). */
data class LockPreviewData(val catName: String, val streak: Int, val levels: CatLevels?, val coupleLine: String, val me: String, val partner: String)

@Composable
private fun SheetHandle() {
    Box(Modifier.padding(top = 12.dp).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(Outline))
}

/**
 * P4 "Plus kilidi" (board PlusKilit): a free user tapped something that is Plus. A preview of that very thing,
 * what it is, the two prices and "Planları gör" (P3 without the countdown). Never shown during heartbeat,
 * drawing or letter writing, and basic cat care is never locked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusLockSheet(feature: PlusFeature, preview: LockPreviewData, prices: PlusPrices, onPlans: () -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action -> scope.launch { sheet.hide() }.invokeOnCompletion { action() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Cream,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The pills sit above the preview, not on it, so they never cover the cat's name on narrow phones.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlusPill(stringResource(R.string.plus_pill), fontSize = 11)
                    Text(
                        stringResource(R.string.plus_lock_preview), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(CreamCard).padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                when (feature) {
                    PlusFeature.WideWidget -> WidePreview(preview)
                    PlusFeature.HeartWidget -> HeartPreview(preview)
                }
            }
            Text(
                stringResource(if (feature == PlusFeature.WideWidget) R.string.plus_lock_wide_title else R.string.plus_lock_heart_title),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = Brown, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                if (feature == PlusFeature.WideWidget) stringResource(R.string.plus_lock_wide_body, TurkishSuffix.genitive(preview.catName), prices.monthly, prices.yearly)
                else stringResource(R.string.plus_lock_heart_body, prices.monthly, prices.yearly),
                fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            PeachButton(stringResource(R.string.plus_lock_plans), { hideThen(onPlans) })
            GhostButton(stringResource(R.string.plus_later), { hideThen(onDismiss) }, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun WidePreview(d: LockPreviewData) {
    val shape = RoundedCornerShape(28.dp)
    Row(Modifier.fillMaxWidth().height(160.dp).softShadow(shape, 12.dp).clip(shape).background(Cream)) {
        Box(Modifier.width(140.dp).fillMaxHeight().background(CatBg)) {
            Image(
                painterResource(if (d.levels?.sleeping == true) R.drawable.cat_sleep else R.drawable.cat_base), null,
                contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, 0.1f), modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)) {
            // Name and streak share one row so all four need bars fit in the 160dp card.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    d.catName, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    pluralStringResource(R.plurals.widget_streak, d.streak, d.streak), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown,
                    maxLines = 1, softWrap = false,
                    modifier = Modifier.clip(CircleShape).background(PeachSoft).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            MiniBar(stringResource(R.string.need_hunger), d.levels?.hunger)
            MiniBar(stringResource(R.string.need_thirst), d.levels?.thirst)
            MiniBar(stringResource(R.string.need_energy), d.levels?.energy)
            MiniBar(stringResource(R.string.need_love), d.levels?.love)
            Text(d.coupleLine, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniBar(label: String, level: Double?) {
    val v = ((level ?: 80.0) / 100.0).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brown, maxLines = 1, modifier = Modifier.width(42.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Sand)) {
            Box(Modifier.fillMaxWidth(v).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if ((level ?: 80.0) < CatRules.LOW) NeedLow else NeedOk))
        }
    }
}

@Composable
private fun HeartPreview(d: LockPreviewData) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().height(96.dp).softShadow(shape, 12.dp).clip(shape).background(Cream).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(74.dp)) {
            InitialAvatar(d.me, Peach, 44.dp, 17.sp)
            InitialAvatar(d.partner, Mint, 44.dp, 17.sp, Modifier.padding(start = 30.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.widget_together), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft)
            Text(d.coupleLine, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(48.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
            SvgIcon(HEART_D, Brown, Modifier.size(22.dp))
        }
    }
}

/**
 * P10 "Plus bitiyor: sen devam ettir" (board PlusDevamEttir): the buyer's subscription won't renew (or ended)
 * and it is the last day; the other partner can continue it on their own Google account. Only opened when
 * PlusRules.canPartnerTakeOver is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeoverSheet(vm: PlusViewModel, plus: PlusInfo, buyerName: String, now: Long, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val prices = plusPrices(catalog)
    var yearly by rememberSaveable { mutableStateOf(true) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val until = plus.until ?: now
    val over = now >= until
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.takeoverShown() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Cream,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlusPill(stringResource(R.string.plus_pill), fontSize = 12)
                Text(
                    if (over) stringResource(R.string.plus_takeover_title_over) else stringResource(R.string.plus_ends_on, TurkishSuffix.locative(shortDate(until))),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft,
                )
            }
            Text(
                stringResource(if (over) R.string.plus_takeover_title_over else R.string.plus_takeover_title_soon),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Brown,
            )
            Text(
                if (over) stringResource(R.string.plus_takeover_body_over, TurkishSuffix.genitive(buyerName))
                else stringResource(R.string.plus_takeover_body, TurkishSuffix.genitive(buyerName), TurkishSuffix.locative(formatDate(until))),
                fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, color = BrownSoft,
            )
            PlanOption(
                selected = yearly, title = stringResource(R.string.plus_yearly), sub = stringResource(R.string.plus_yearly_sub),
                price = prices.yearly, priceSub = stringResource(R.string.plus_per_year), onClick = { yearly = true },
            )
            PlanOption(
                selected = !yearly, title = stringResource(R.string.plus_monthly), sub = stringResource(R.string.plus_monthly_sub),
                price = prices.monthly, priceSub = stringResource(R.string.plus_per_month), onClick = { yearly = false },
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kit.MintCell).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InitialAvatar(buyerName, Peach, 32.dp, 13.sp, border = false)
                Text(
                    stringResource(R.string.plus_takeover_note, TurkishSuffix.ablative(buyerName)),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = Brown, modifier = Modifier.weight(1f),
                )
            }
            PeachButton(
                stringResource(R.string.plus_takeover_buy, if (yearly) prices.yearly else prices.monthly),
                onClick = { vm.buy(context.findActivity(), if (yearly) PlusPlan.Yearly else PlusPlan.Monthly) },
                loading = busy,
            )
            Text(
                if (yearly) stringResource(R.string.plus_disc_yearly, prices.yearly) else stringResource(R.string.plus_disc_monthly, prices.monthly),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            GhostButton(
                stringResource(R.string.plus_later),
                { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } },
                Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
