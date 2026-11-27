package com.lovelyy.app.ui.plus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.billing.PlusCatalog
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.text.AppLocale
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.SvgIcon
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand

// ---------- app-wide hook ----------

/** Things that can be locked behind Plus. [key] goes into analytics ("lock_<key>"), never anything personal. */
enum class PlusFeature(val key: String) {
    WideWidget("wide_widget"),
    HeartWidget("heart_widget");

    companion object {
        fun of(key: String?): PlusFeature? = entries.firstOrNull { it.key == key }
    }
}

/**
 * What any screen under Home needs to know about Plus (provided by HomeScreen through [LocalPlus]): the current
 * map, the time it was evaluated at and how to open the lock sheet (P4) or the plans (P3). Later Plus features
 * (free-tier limits, streak protection, decorations) plug in here instead of observing Firestore themselves.
 */
@Immutable
data class PlusAccess(
    val info: PlusInfo?,
    val me: String,
    val now: Long,
    val openLock: (PlusFeature) -> Unit,
    val openPlans: (source: String) -> Unit,
) {
    val isPlus: Boolean get() = PlusRules.isPlus(info, now)
}

/** Null outside Home (onboarding, pairing): treat as "no Plus, no lock". */
val LocalPlus = compositionLocalOf<PlusAccess?> { null }

// ---------- prices ----------

/** Play prices when loaded, else the approved launch prices from string resources. */
data class PlusPrices(val yearly: String, val firstYear: String, val monthly: String, val gift: String)

@Composable
fun plusPrices(c: PlusCatalog): PlusPrices = PlusPrices(
    yearly = c.yearly ?: stringResource(R.string.plus_price_yearly_fallback),
    firstYear = c.yearlyFirstYear ?: stringResource(R.string.plus_price_first_year_fallback),
    monthly = c.monthly ?: stringResource(R.string.plus_price_monthly_fallback),
    gift = c.gift ?: stringResource(R.string.plus_price_gift_fallback),
)

/** "28 Kasım" (day and month in the app language). */
fun shortDate(millis: Long): String =
    com.lovelyy.app.data.DayCounter.dateOf(millis).format(AppLocale.formatter("dMMMM"))

/** "47:12:08" for the offer countdown. */
fun countdown(millis: Long): String {
    val s = (millis / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}

/** The Activity behind a Compose context (Google Play's purchase window needs one). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

// ---------- icons (24×24 design paths) ----------

internal object PlusIcons {
    const val SPARKLE = "M12 2l2.6 7.4L22 12l-7.4 2.6L12 22l-2.6-7.4L2 12l7.4-2.6z"
    const val GIFT = "M6 10h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z M3 7h18v3H3z M12 7v13 M12 7c-1.5-3-5-3-5-1s3 1 5 1z M12 7c1.5-3 5-3 5-1s-3 1-5 1z"
    const val SHIELD = "M12 3l7 3v5c0 5-3.2 8.3-7 10-3.8-1.7-7-5-7-10V6z M9 12l2.2 2.2L15 10.5"
    const val SOFA = "M5 11V8a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v3 M3 13a2 2 0 0 1 4 0v2h10v-2a2 2 0 0 1 4 0v5H3z M6 18v2 M18 18v2"
    const val GRID = "M6 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M15 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M6 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z M15 13h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z"
    const val BUBBLE = "M5 5h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7l-5 4v-4H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z"
    const val BRUSH = "M14 4l6 6-8 8H6v-6z M6 18c-1.5 0-3 1-3 3 2 0 3-1 3-3z"
    const val CLOCK = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17z M12 7.5V12l3 2"
    const val CHEVRON = "M9 5l7 7-7 7"
    const val EXTERNAL = "M14 4h6v6 M20 4l-9 9 M18 14v4a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4"
    const val RESTORE = "M4 12a8 8 0 1 0 2.3-5.7 M4 4v4h4"
}

// ---------- shared pieces ----------

/** The dark "✦ Lovelyy Plus" pill. */
@Composable
fun PlusPill(text: String, modifier: Modifier = Modifier, fontSize: Int = 13) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Brown).padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SvgIcon(PlusIcons.SPARKLE, Cream, Modifier.size(fontSize.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, color = Cream, maxLines = 1)
    }
}

/** Icon in a tinted circle, a bold title and a small line (P1, P3 lists). */
@Composable
fun BenefitRow(icon: String, tint: Color, title: String, sub: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            if (sub != null) Text(sub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = BrownSoft)
        }
    }
}

/** A plan card with a radio dot (P3, P8, P10). [badge] sits on the top edge (the offer countdown). */
@Composable
fun PlanOption(
    selected: Boolean,
    title: String,
    sub: String,
    price: String,
    priceSub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    strikePrice: String? = null,
    badge: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.fillMaxWidth().padding(top = if (badge != null) 12.dp else 0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(shape)
                .background(if (selected) PeachTint else CreamCard)
                .border(if (selected) 2.5.dp else 1.5.dp, if (selected) PeachDeep else Outline, shape)
                .semantics { role = Role.RadioButton; this.selected = selected }
                .bounceClick(pressedScale = 0.98f, onClick = onClick)
                .padding(start = 14.dp, end = 14.dp, top = if (badge != null) 18.dp else 12.dp, bottom = if (badge != null) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioDot(selected)
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown)
                Text(sub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = Kit.CellLabel)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (strikePrice != null) {
                    Text(strikePrice, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, textDecoration = TextDecoration.LineThrough)
                }
                Text(price, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown)
                Text(priceSub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Kit.CellLabel)
            }
        }
        if (badge != null) {
            Row(
                Modifier.offset(x = 14.dp, y = (-12).dp).clip(RoundedCornerShape(50)).background(Brown).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                SvgStrokeIcon(PlusIcons.CLOCK, Cream, Modifier.size(13.dp), strokeWidth = 2.4f)
                Text(badge, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Cream, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    if (selected) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Brown), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Cream))
        }
    } else {
        Box(Modifier.size(22.dp).clip(CircleShape).background(CreamCard).border(2.dp, Kit.Dashed, CircleShape))
    }
}

/** A tappable row in a white card: icon, title + sub, chevron (P8 links). */
@Composable
fun PlusLinkRow(icon: String, title: String, sub: String, onClick: () -> Unit, trailing: String = PlusIcons.CHEVRON) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).bounceClick(pressedScale = 0.98f, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SvgStrokeIcon(icon, Brown, Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            Text(sub, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft)
        }
        SvgStrokeIcon(trailing, BrownSoft, Modifier.size(16.dp))
    }
}

/** Small check + text line (P8 "what's in Plus" list). */
@Composable
fun FeatureLine(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        SvgStrokeIcon(Kit.CHECK_D, Kit.Green, Modifier.size(18.dp), strokeWidth = 2.4f)
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp, color = Brown)
    }
}

/** Plan label for a status card ("Plus · yıllık"). */
@StringRes
fun planLabel(plan: com.lovelyy.app.data.PlusPlan?): Int = when (plan) {
    com.lovelyy.app.data.PlusPlan.Monthly -> R.string.plus_plan_monthly
    com.lovelyy.app.data.PlusPlan.Gift -> R.string.plus_plan_gift
    else -> R.string.plus_plan_yearly
}
