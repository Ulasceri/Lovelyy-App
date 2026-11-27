package com.lovelyy.app.ui.plus

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.PlusClock
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay

/** Paywall sources (analytics "source" values, also decide the title). */
object PaywallSource {
    const val TRIAL_END = "trial_end"
    const val BIZ_CARD = "biz_card"
    const val HOME_CARD = "home_card"
    const val MANAGEMENT = "management"
    fun lock(feature: PlusFeature) = "lock_${feature.key}"
    /** P5a "İzin günü kullan" or P5c "Plus ile telafi et" tapped without Plus. */
    const val STREAK_DAY_OFF = "lock_streak_day_off"
    const val STREAK_REPAIR = "lock_streak_repair"
    /** P9: adding to a list that is at its free limit. */
    fun limit(limit: com.lovelyy.app.data.PlusLimits.Limit) = "lock_${limit.key}"
}

/**
 * P3 "Deneme sonu: planlar" (board PlusPaywall). Full screen, no bottom menu. Opens by itself once when the
 * trial ended (PlusRules.shouldShowTrialEndPaywall), or from P4 / the Home card without the "you'll lose" title.
 * Yearly is preselected; its first-year price and live countdown only show in the 48 h offer window. The X keeps
 * the free version. Restore, terms, privacy and the auto-renew disclosure are always visible (Play policy).
 */
@Composable
fun PaywallScreen(
    vm: PlusViewModel,
    plus: PlusInfo?,
    streak: Int,
    source: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val prices = plusPrices(catalog)
    var now by remember { mutableLongStateOf(PlusClock.now(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = PlusClock.now(context)
        }
    }
    LaunchedEffect(source) { vm.paywallShown(source) }
    var yearly by rememberSaveable { mutableStateOf(true) }
    val offer = PlusRules.firstYearOfferActive(plus, now)
    val trialEnd = source == PaywallSource.TRIAL_END

    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlusPill(stringResource(R.string.plus_name))
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Close, stringResource(R.string.plus_paywall_close), onClose)
            }
            Text(
                stringResource(if (trialEnd) R.string.plus_paywall_title_trial else R.string.plus_paywall_title),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = Brown,
                modifier = Modifier.padding(top = 12.dp),
            )
            SoftCard(Modifier.fillMaxWidth().padding(top = 14.dp), padding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (trialEnd) R.string.plus_paywall_lose else R.string.plus_paywall_gain),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft,
                    )
                    if (streak > 0) {
                        BenefitRow(PlusIcons.SHIELD, PeachTint, pluralStringResource(R.plurals.plus_row_streak_count, streak, streak), stringResource(R.string.plus_row_streak_sub))
                    } else {
                        BenefitRow(PlusIcons.SHIELD, PeachTint, stringResource(R.string.plus_b_streak), stringResource(R.string.plus_b_streak_sub))
                    }
                    BenefitRow(PlusIcons.BRUSH, MintTint, stringResource(R.string.plus_row_drawing), stringResource(R.string.plus_row_drawing_sub))
                    BenefitRow(PlusIcons.GRID, PeachTint, stringResource(R.string.plus_row_widget), stringResource(R.string.plus_row_widget_sub))
                }
            }
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    selected = !yearly,
                    title = stringResource(R.string.plus_monthly),
                    sub = stringResource(R.string.plus_monthly_sub),
                    price = prices.monthly,
                    priceSub = stringResource(R.string.plus_per_month),
                    onClick = { yearly = false },
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SvgIcon(HEART_D, Peach, Modifier.size(15.dp))
                Text(stringResource(R.string.plus_one_sub_long), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
            }
            Spacer(Modifier.height(16.dp))
        }
        val price = if (yearly) (if (offer) prices.firstYear else prices.yearly) else prices.monthly
        PeachButton(
            stringResource(if (trialEnd) R.string.plus_buy_continue else R.string.plus_buy_start, price),
            onClick = { vm.buy(context.findActivity(), if (yearly) PlusPlan.Yearly else PlusPlan.Monthly) },
            loading = busy,
        )
        Text(
            when {
                !yearly -> stringResource(R.string.plus_disc_monthly, prices.monthly)
                offer -> stringResource(R.string.plus_disc_yearly_offer, prices.firstYear, prices.yearly)
                else -> stringResource(R.string.plus_disc_yearly, prices.yearly)
            },
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        LegalLinks(onRestore = vm::restore)
    }
}

/** "Satın alımları geri yükle · Kullanım şartları · Gizlilik" (44dp touch targets). */
@Composable
fun LegalLinks(onRestore: () -> Unit) {
    val context = LocalContext.current
    val terms = stringResource(R.string.plus_terms_url)
    val privacy = stringResource(R.string.plus_privacy_url)
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(context, url, Toast.LENGTH_SHORT).show() }
    }
    // Restore on its own line, terms and privacy together below: no wrap can leave a lone "·".
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LinkText(stringResource(R.string.plus_restore), onRestore)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            LinkText(stringResource(R.string.plus_terms)) { open(terms) }
            Dot()
            LinkText(stringResource(R.string.plus_privacy)) { open(privacy) }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).bounceClick(onClick = onClick).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
        )
    }
}

@Composable
private fun Dot() {
    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        Text("·", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft)
    }
}
