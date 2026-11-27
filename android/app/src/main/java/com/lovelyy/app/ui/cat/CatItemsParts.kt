package com.lovelyy.app.ui.cat

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lovelyy.app.R
import com.lovelyy.app.data.CareSlot
import com.lovelyy.app.data.CatItem
import com.lovelyy.app.data.CatItems
import com.lovelyy.app.ui.theme.Brown

/**
 * Where each care slot's item stands around the cat (boards P7a/P7b, measured on a 350 × 236 video). Two depth
 * rows so the items stand on the floor instead of floating: water and bed at the back, food and toy in front,
 * partly over them. Every item gets a soft contact shadow drawn by the app (the pictures have none).
 * [x], [top], [size] place the picture; [shadow*] the ellipse under it. All in board pixels. The bed sits 9 px
 * further left than on the board so it is not cut at the card's right edge.
 */
private class SlotPlace(
    val slot: CareSlot,
    val x: Float, val top: Float, val size: Float,
    val shadowX: Float, val shadowTop: Float, val shadowW: Float, val shadowH: Float,
)

private const val BOARD_W = 350f
private const val BOARD_H = 236f

/** Back to front. */
private val PLACES = listOf(
    SlotPlace(CareSlot.Water, -1f, 175f, 62f, 6f, 209f, 49f, 11f),
    SlotPlace(CareSlot.Feed, 14f, 184f, 72f, 21f, 224f, 58f, 13f),
    SlotPlace(CareSlot.Sleep, 243f, 144f, 104f, 239f, 205f, 111f, 24f),
    SlotPlace(CareSlot.Play, 219f, 156f, 86f, 235f, 225f, 54f, 12f),
)

/**
 * Mırmır's items over the bottom of the cat video (P7b on Home, the P7a preview). Positions scale with the width
 * and hang from the bottom edge, so a taller or shorter video keeps them on the floor. [hidden]: the slots whose
 * items are inside, or covered by, the clip playing right now. [pop]: the item that was just placed
 * lands with a small bounce.
 */
@Composable
fun CatItemsLayer(items: CatItems, modifier: Modifier = Modifier, hidden: Set<CareSlot> = emptySet(), pop: CatItem? = null) {
    BoxWithConstraints(modifier) {
        val k = maxWidth / BOARD_W
        val h = maxHeight
        // Hang from the bottom edge: the board's distance from the bottom, scaled.
        fun fromBottom(top: Float): Dp = h - k * (BOARD_H - top)
        PLACES.forEach { p ->
            val item = items[p.slot]
            key(p.slot) {
                AnimatedVisibility(
                    visible = p.slot !in hidden,
                    enter = if (pop == item) scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), initialScale = 0.6f) + fadeIn()
                    else fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box {
                        ItemShadow(
                            Modifier
                                .offset(x = k * p.shadowX, y = fromBottom(p.shadowTop))
                                .size(k * p.shadowW, k * p.shadowH),
                        )
                        Image(
                            painterResource(item.drawable),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = k * p.x, y = fromBottom(p.top))
                                .size(k * p.size),
                        )
                    }
                }
            }
        }
    }
}

/** The soft ellipse under an item (board: radial gradient, brown at 34 % fading out at 72 %). */
@Composable
private fun ItemShadow(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.width / 2f
        scale(scaleX = 1f, scaleY = size.height / size.width, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawCircle(
                Brush.radialGradient(
                    0f to Brown.copy(alpha = 0.34f),
                    0.72f to Color.Transparent,
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}

@get:DrawableRes
val CatItem.drawable: Int
    get() = when (this) {
        CatItem.MamaSade -> R.drawable.item_mama_sade
        CatItem.MamaBalikTabagi -> R.drawable.item_mama_balik_tabagi
        CatItem.MamaSut -> R.drawable.item_mama_sut
        CatItem.SuSade -> R.drawable.item_su_sade
        CatItem.SuCesme -> R.drawable.item_su_cesme
        CatItem.OyuncakYumak -> R.drawable.item_oyuncak_yumak
        CatItem.OyuncakFare -> R.drawable.item_oyuncak_fare
        CatItem.OyuncakTuy -> R.drawable.item_oyuncak_tuy
        CatItem.OyuncakTirmalama -> R.drawable.item_oyuncak_tirmalama
        CatItem.YatakMinder -> R.drawable.item_yatak_minder
        CatItem.YatakKutu -> R.drawable.item_yatak_kutu
        CatItem.YatakSepet -> R.drawable.item_yatak_sepet
    }

@get:StringRes
val CatItem.nameRes: Int
    get() = when (this) {
        CatItem.MamaSade -> R.string.item_mama_sade
        CatItem.MamaBalikTabagi -> R.string.item_mama_balik_tabagi
        CatItem.MamaSut -> R.string.item_mama_sut
        CatItem.SuSade -> R.string.item_su_sade
        CatItem.SuCesme -> R.string.item_su_cesme
        CatItem.OyuncakYumak -> R.string.item_oyuncak_yumak
        CatItem.OyuncakFare -> R.string.item_oyuncak_fare
        CatItem.OyuncakTuy -> R.string.item_oyuncak_tuy
        CatItem.OyuncakTirmalama -> R.string.item_oyuncak_tirmalama
        CatItem.YatakMinder -> R.string.item_yatak_minder
        CatItem.YatakKutu -> R.string.item_yatak_kutu
        CatItem.YatakSepet -> R.string.item_yatak_sepet
    }

/** Mırmır's line when the item is placed for the first time (one full sentence per item, for translations). */
@get:StringRes
val CatItem.reactionRes: Int
    get() = when (this) {
        CatItem.MamaSade -> R.string.item_love_mama_sade
        CatItem.MamaBalikTabagi -> R.string.item_love_mama_balik_tabagi
        CatItem.MamaSut -> R.string.item_love_mama_sut
        CatItem.SuSade -> R.string.item_love_su_sade
        CatItem.SuCesme -> R.string.item_love_su_cesme
        CatItem.OyuncakYumak -> R.string.item_love_oyuncak_yumak
        CatItem.OyuncakFare -> R.string.item_love_oyuncak_fare
        CatItem.OyuncakTuy -> R.string.item_love_oyuncak_tuy
        CatItem.OyuncakTirmalama -> R.string.item_love_oyuncak_tirmalama
        CatItem.YatakMinder -> R.string.item_love_yatak_minder
        CatItem.YatakKutu -> R.string.item_love_yatak_kutu
        CatItem.YatakSepet -> R.string.item_love_yatak_sepet
    }
