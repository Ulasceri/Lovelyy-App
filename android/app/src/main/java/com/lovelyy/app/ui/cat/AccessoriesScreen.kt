package com.lovelyy.app.ui.cat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.CareRewards
import com.lovelyy.app.data.CareRewards.Accessory
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.history.InnerColumn
import com.lovelyy.app.ui.history.Muted
import com.lovelyy.app.ui.history.OutChip
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import java.time.LocalDate

private val CatBg = Color(0xFFFAE2C0)
private val LockedRing = Color(0xFFB8A69D)
private val OpenText = Color(0xFFA8501F)
private val WornText = Color(0xFF1F6B57)
private val ButtonOff = Color(0xFFF3EAE3)
private const val FLAME_D = "M12 21c-4 0-6-2.7-6-6 0-4 4-6 4-10 3 2 4 4 4 6 1-1 1.5-2 1.5-3 2 2 2.5 4.5 2.5 7 0 3.3-2 6-6 6z"

private fun iconOf(a: Accessory) = when (a) {
    Accessory.Kurdele -> "M12 10c-2-3-7-4-8-2s1 5 3 5 4-1 5-3zM12 10c2-3 7-4 8-2s-1 5-3 5-4-1-5-3zM11 12l-3 8M13 12l3 8"
    Accessory.Papyon -> "M10 12L3 7v10zM14 12l7-5v10zM10 10h4v4h-4z"
    Accessory.Sapka -> "M3 17h18M6 17v-5a6 6 0 0 1 12 0v5M6 13.5h12"
    Accessory.Gozluk -> "M3.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0zM13.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0zM10.5 12.5c.9-.7 2.1-.7 3 0M3.5 12L2 9M20.5 12L22 9"
}

/** "Kurdeleyi", "Papyonu"... for "X'i Mırmır'a tak". */
private fun accusativeRes(a: Accessory) = when (a) {
    Accessory.Kurdele -> R.string.ac_acc_kurdele
    Accessory.Papyon -> R.string.ac_acc_papyon
    Accessory.Sapka -> R.string.ac_acc_sapka
    Accessory.Gozluk -> R.string.ac_acc_gozluk
}

/**
 * Aksesuarlar (approved board 32): try each accessory on the cat, see which are unlocked by the shared care streak,
 * and put one on (both partners see it). Unlocking uses the best streak, like Kedi bakımı.
 */
@Composable
fun AccessoriesScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val doc = ui?.doc
    val best = doc?.streak?.best ?: 0
    val current = doc?.streak?.current(LocalDate.now()) ?: 0
    val worn = doc?.let { CareRewards.worn(it.wearing, best) }
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val catName = doc?.name?.takeIf { it.isNotBlank() }
    var selName by rememberSaveable { mutableStateOf<String?>(null) }
    val sel = Accessory.entries.firstOrNull { it.name == selName } ?: worn ?: Accessory.entries.first()
    val selUnlocked = CareRewards.unlocked(sel, best)
    val selWorn = sel == worn
    val selLabel = stringResource(accessoryNameRes(sel))

    InnerColumn(spacing = 12) {
        CenteredTopBar(stringResource(R.string.cc_accessories), onBack)

        // Cat preview with the selected accessory (locked ones can be previewed too).
        val shape = RoundedCornerShape(28.dp)
        BoxWithConstraints(Modifier.fillMaxWidth().height(280.dp).softShadow(shape, 10.dp).clip(shape).background(CatBg)) {
            val side = maxWidth
            Box(Modifier.size(side).offset(y = -side * 0.05f)) {
                Image(painterResource(R.drawable.cat_base), stringResource(R.string.ac_cat_desc, catName ?: stringResource(R.string.cat_generic)), Modifier.fillMaxSize())
                CatAccessoryLayer(sel, Modifier.fillMaxSize())
            }
            Text(
                when {
                    !selUnlocked -> stringResource(R.string.ac_preview_locked, selLabel)
                    selWorn -> stringResource(R.string.ac_worn_label, selLabel)
                    else -> stringResource(R.string.ac_preview, selLabel)
                },
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Row(
                Modifier.align(Alignment.TopEnd).padding(14.dp).clip(CircleShape).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SvgStrokeIcon(FLAME_D, Brown, Modifier.size(14.dp))
                Text(stringResource(R.string.ac_streak, current), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown)
            }
        }

        Accessory.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a ->
                    AccessoryTile(a, CareRewards.unlocked(a, best), a == worn, a == sel, current, Modifier.weight(1f)) { selName = a.name }
                }
            }
        }

        Text(
            stringResource(R.string.ac_note, catName?.let { TurkishSuffix.dative(it) } ?: stringResource(R.string.ac_note_cat), partnerName),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp, color = SubText,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )

        val accusative = stringResource(accusativeRes(sel))
        val label = when {
            !selUnlocked -> stringResource(R.string.ac_opens_in, (sel.day - current).coerceAtLeast(1))
            selWorn -> stringResource(R.string.ac_take_off, accusative)
            catName != null -> stringResource(R.string.ac_put_on_named, accusative, TurkishSuffix.dative(catName))
            else -> stringResource(R.string.ac_put_on, accusative)
        }
        val btnShape = RoundedCornerShape(28.dp)
        Box(
            Modifier.fillMaxWidth().height(56.dp)
                .then(if (selUnlocked) Modifier.softShadow(btnShape, 8.dp) else Modifier)
                .clip(btnShape).background(if (selUnlocked) Peach else ButtonOff)
                .semantics { role = Role.Button }
                .bounceClick(enabled = selUnlocked) { vm.wear(if (selWorn) null else sel) },
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (selUnlocked) Brown else Muted)
        }
    }
}

@Composable
private fun AccessoryTile(a: Accessory, unlocked: Boolean, worn: Boolean, selected: Boolean, current: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .then(if (selected) Modifier.border(3.dp, Brown, shape) else Modifier.softShadow(shape, 6.dp))
            .clip(shape).background(Color.White)
            .semantics { this.selected = selected; role = Role.RadioButton }
            .bounceClick(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            if (unlocked) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(if (worn) Mint else OutChip))
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 2.dp.toPx()
                    drawCircle(LockedRing, radius = size.minDimension / 2 - w / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                }
            }
            SvgStrokeIcon(iconOf(a), if (unlocked) Brown else Muted, Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(stringResource(accessoryNameRes(a)), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1)
            Text(
                stringResource(if (!unlocked) R.string.ac_locked else if (worn) R.string.ac_worn else R.string.ac_open),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp,
                color = if (!unlocked) Muted else if (worn) WornText else OpenText,
            )
            Text(
                if (unlocked) stringResource(R.string.ac_opened_day, a.day) else stringResource(R.string.ac_days_left, (a.day - current).coerceAtLeast(1)),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = Muted, maxLines = 1,
            )
        }
    }
}
