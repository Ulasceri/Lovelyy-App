package com.lovelyy.app.ui.plus

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.BuildConfig
import com.lovelyy.app.R
import com.lovelyy.app.billing.BillingRepository
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.data.PlusSource
import com.lovelyy.app.data.PlusStatus
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.ChoicePill
import com.lovelyy.app.ui.components.CoupleAvatars
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.home.formatDate
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand
import kotlin.math.ceil

/**
 * P8 "Plus yönetimi" (board PlusYonetim) and P8b for the partner who did not buy (board PlusYonetimPartner).
 * The buyer manages the subscription in Google Play; the other partner sees who manages it, and gets
 * "Plus'ı ben devam ettireyim" (P10) only when PlusRules.canPartnerTakeOver. During the trial or after Plus
 * ended, the plans are shown under the status card.
 */
@Composable
fun PlusManageScreen(
    vm: PlusViewModel,
    me: String,
    myName: String,
    partnerName: String,
    catName: String,
    plus: PlusInfo?,
    now: Long,
    onBack: () -> Unit,
    onGift: () -> Unit,
    onTakeover: () -> Unit,
) {
    val context = LocalContext.current
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val prices = plusPrices(catalog)
    val status = PlusRules.status(plus, me, now)
    val canBuy = PlusRules.canPurchase(plus, now)
    val takeover = PlusRules.canPartnerTakeOver(plus, me, now)
    var yearly by rememberSaveable { mutableStateOf(true) }
    val offer = PlusRules.firstYearOfferActive(plus, now)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 8.dp)) {
        CenteredTopBar(stringResource(R.string.plus_manage_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (status) {
                is PlusStatus.Paid -> PaidCard(status, plus!!, myName, partnerName, prices, now, takeover, onTakeover)
                is PlusStatus.Trial -> BizPlusCard(status, onClick = null, showMore = false)
                else -> SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlusPill(stringResource(R.string.plus_pill), fontSize = 12)
                        Spacer(Modifier.weight(1f))
                        Text(stringResource(R.string.plus_state_off), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                    }
                    Text(stringResource(R.string.plus_free_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown, modifier = Modifier.padding(top = 10.dp))
                    Text(stringResource(R.string.plus_free_sub), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = BrownSoft, modifier = Modifier.padding(top = 4.dp))
                    if (takeover) {
                        Spacer(Modifier.height(12.dp))
                        PeachButton(stringResource(R.string.plus_takeover_button), onTakeover, height = 52.dp)
                    }
                }
            }

            // Plans: during the trial, after Plus ended, or on the last day of my own non-renewing period.
            val showPlans = canBuy && !takeover && (status !is PlusStatus.Paid || status.boughtByMe)
            if (showPlans) {
                Text(stringResource(R.string.plus_plans), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown, modifier = Modifier.padding(top = 6.dp))
                PlanOption(
                    selected = yearly,
                    title = stringResource(R.string.plus_yearly),
                    sub = if (offer) stringResource(R.string.plus_yearly_offer_sub, prices.yearly) else stringResource(R.string.plus_yearly_sub),
                    price = if (offer) prices.firstYear else prices.yearly,
                    priceSub = stringResource(if (offer) R.string.plus_first_year else R.string.plus_per_year),
                    strikePrice = if (offer) prices.yearly else null,
                    badge = if (offer) stringResource(R.string.plus_offer_badge, countdown(PlusRules.offerMillisLeft(plus, now))) else null,
                    onClick = { yearly = true },
                )
                PlanOption(
                    selected = !yearly, title = stringResource(R.string.plus_monthly), sub = stringResource(R.string.plus_monthly_sub),
                    price = prices.monthly, priceSub = stringResource(R.string.plus_per_month), onClick = { yearly = false },
                )
                Text(stringResource(R.string.plus_one_sub_long), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                val price = if (yearly) (if (offer) prices.firstYear else prices.yearly) else prices.monthly
                PeachButton(
                    stringResource(R.string.plus_buy_start, price),
                    onClick = { vm.buy(context.findActivity(), if (yearly) PlusPlan.Yearly else PlusPlan.Monthly) },
                    loading = busy,
                )
                Text(
                    when {
                        !yearly -> stringResource(R.string.plus_disc_monthly, prices.monthly)
                        offer -> stringResource(R.string.plus_disc_yearly_offer, prices.firstYear, prices.yearly)
                        else -> stringResource(R.string.plus_disc_yearly, prices.yearly)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // What Plus has.
            SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                FeatureLine(stringResource(R.string.plus_f1))
                FeatureLine(stringResource(R.string.plus_f2, TurkishSuffix.genitive(catName)))
                FeatureLine(stringResource(R.string.plus_f3))
                FeatureLine(stringResource(R.string.plus_f4))
                FeatureLine(stringResource(R.string.plus_f5))
                FeatureLine(stringResource(R.string.plus_f6))
            }

            // Links.
            SoftCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                if (status is PlusStatus.Paid && status.boughtByMe && status.source == PlusSource.Sub) {
                    PlusLinkRow(PlusIcons.EXTERNAL, stringResource(R.string.plus_manage_link), stringResource(R.string.plus_manage_link_sub), onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BillingRepository.manageUrl(context.packageName)))) }
                    })
                    Divider()
                }
                if (canBuy) {
                    PlusLinkRow(PlusIcons.GIFT, stringResource(R.string.plus_gift_top), stringResource(R.string.plus_gift_cta_sub), onClick = onGift)
                    Divider()
                }
                PlusLinkRow(PlusIcons.RESTORE, stringResource(R.string.plus_restore), stringResource(R.string.plus_restore_sub), onClick = vm::restore)
            }
            LegalLinks(onRestore = vm::restore)

            val footer = when {
                status is PlusStatus.Paid && status.boughtByMe -> stringResource(R.string.plus_manage_footer)
                status is PlusStatus.Paid && status.autoRenew -> stringResource(R.string.plus_partner_footer, TurkishSuffix.genitive(partnerName))
                else -> null
            }
            if (footer != null) {
                Text(footer, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            if (BuildConfig.DEBUG) DebugBox(vm)
        }
    }
}

@Composable
private fun PaidCard(
    status: PlusStatus.Paid,
    plus: PlusInfo,
    myName: String,
    partnerName: String,
    prices: PlusPrices,
    now: Long,
    takeover: Boolean,
    onTakeover: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PeachSoft).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlusPill(stringResource(planLabel(status.plan)), fontSize = 12)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (status.autoRenew) R.string.plus_state_active else R.string.plus_state_ending),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (status.autoRenew) Kit.Green else Kit.CellLabel,
            )
        }
        Text(
            stringResource(if (status.autoRenew) R.string.plus_renews_on else R.string.plus_ends_on, TurkishSuffix.locative(formatDate(status.until))),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = Brown,
        )
        val daysLeft = ceil((status.until - now).toDouble() / PlusRules.DAY_MS).toInt().coerceAtLeast(1)
        Text(
            when {
                !status.autoRenew -> pluralStringResource(R.plurals.plus_ends_left, daysLeft, daysLeft)
                status.plan == PlusPlan.Monthly -> stringResource(R.string.plus_price_month, prices.monthly)
                else -> stringResource(R.string.plus_price_year, prices.yearly)
            },
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Kit.CellLabel,
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Sand))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (status.boughtByMe) CoupleAvatars(myName, partnerName, size = 36.dp, overlap = 12.dp, fontSize = 14.sp)
            else InitialAvatar(partnerName, com.lovelyy.app.ui.theme.Mint, 36.dp, 14.sp, border = false)
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        status.boughtByMe && status.isGift -> stringResource(R.string.plus_gifted_by_me)
                        status.boughtByMe -> stringResource(R.string.plus_bought_by_me)
                        status.isGift -> stringResource(R.string.plus_gifted_by, partnerName)
                        else -> stringResource(R.string.plus_bought_by, partnerName)
                    },
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
                )
                Text(
                    if (status.boughtByMe && status.isGift) stringResource(R.string.plus_gifted_by_me_sub, TurkishSuffix.locative(partnerName))
                    else if (status.boughtByMe) stringResource(R.string.plus_bought_by_me_sub, TurkishSuffix.locative(partnerName))
                    else stringResource(R.string.plus_managed_by, partnerName),
                    fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Kit.CellLabel,
                )
            }
        }
        if (takeover) PeachButton(stringResource(R.string.plus_takeover_button), onTakeover, height = 52.dp)
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Sand))

/** Debug builds only: move Plus time and change the fake purchase without Google Play. */
@Composable
private fun DebugBox(vm: PlusViewModel) {
    SoftCard(Modifier.fillMaxWidth(), color = Sand, padding = 14.dp) {
        Text(stringResource(R.string.plus_debug_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
        Text(
            stringResource(R.string.plus_debug_fake, vm.fakeBilling.toString()),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill(stringResource(R.string.plus_debug_day), false, { vm.debugShiftDays(1) })
            ChoicePill(stringResource(R.string.plus_debug_real_time), false, { vm.debugShiftDays(0) })
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoicePill(stringResource(R.string.plus_debug_stop_renew), false, vm::debugStopRenewing)
            ChoicePill(stringResource(R.string.plus_debug_reset), false, vm::debugResetPurchase)
        }
    }
}
