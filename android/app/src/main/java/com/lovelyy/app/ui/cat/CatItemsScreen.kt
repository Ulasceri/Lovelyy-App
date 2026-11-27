package com.lovelyy.app.ui.cat

import android.icu.text.ListFormatter
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.CareSlot
import com.lovelyy.app.data.CatItem
import com.lovelyy.app.data.CatItemRules
import com.lovelyy.app.data.CatItems
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.data.displayCatName
import com.lovelyy.app.text.AppLocale
import com.lovelyy.app.text.TurkishSuffix
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.Kit
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.plus.LocalPlus
import com.lovelyy.app.ui.plus.PaywallSource
import com.lovelyy.app.ui.plus.PlusPill
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownMuted
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CatBg
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand

private const val LABEL_W = 84
private const val TILE_MAX = 58
private const val GAP = 8

/**
 * P7a "Mırmır'ın eşyaları" (approved board PlusKose), opened from Kedi bakımı's top bar. A preview of the cat with
 * the chosen items, then one row per care action: the free model first, then the Plus models, with the Plus bonus
 * at the start of the row. "Yerleştir" saves; with Plus models chosen and no Plus it says "Plus ile yerleştir" and
 * opens the plans instead. After a save Home opens, where Mırmır reacts to the new item (P7b).
 */
@Composable
fun CatItemsScreen(me: UserProfile, onBack: () -> Unit, onSaved: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val vm: CatViewModel = viewModel(key = "cat-$coupleId") { CatViewModel(app, coupleId, me.uid) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val plus = LocalPlus.current
    val isPlus = plus?.isPlus == true
    val context = LocalContext.current
    val view = LocalView.current
    val catName = displayCatName(ui?.doc?.name)
    val stored = ui?.doc?.items

    // The selection survives the paywall and rotation; it starts from what the couple has placed.
    var picked by rememberSaveable { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(ui != null) {
        if (picked == null && ui != null) picked = (stored ?: CatItems()).toMap().values.toList()
    }
    val selection = picked?.let { ids -> CatItems.fromMap(CareSlot.entries.zip(ids).associate { (s, id) -> s.key to id }) }
        ?: stored ?: CatItems()
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.oneTimeEvents.collect { e ->
            when (e) {
                is CatEvent.ItemsSaved -> {
                    saving = false
                    onSaved()
                }
                CatEvent.Offline -> {
                    saving = false
                    Toast.makeText(context, context.getString(R.string.cat_offline, catName), Toast.LENGTH_LONG).show()
                }
                CatEvent.Failed -> {
                    saving = false
                    Toast.makeText(context, context.getString(R.string.cat_error), Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    val needsPlan = selection.needsPlus && !isPlus
    val changed = stored == null || selection != stored

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CenteredTopBar(stringResource(R.string.items_title, TurkishSuffix.genitive(catName)), onBack) {
            PlusPill(stringResource(R.string.items_badge_plus), fontSize = 12)
        }

        ItemsPreview(selection, catName)

        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.items_free_note),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, modifier = Modifier.weight(1f),
            )
            SvgStrokeIcon(Kit.LOCK_D, BrownSoft, Modifier.size(13.dp), strokeWidth = 2.2f)
            Text(
                stringResource(R.string.items_plus_model),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft,
                modifier = Modifier.padding(start = 5.dp),
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Four models in a row must fit 360dp phones too: tiles shrink below 58dp when needed.
            val tile = ((maxWidth - LABEL_W.dp - (GAP * 4).dp) / 4).coerceAtMost(TILE_MAX.dp)
            Column(verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                CareSlot.entries.forEach { slot ->
                    ItemRow(slot, selection[slot], showLocks = !isPlus, tile = tile) { item ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        picked = selection.with(item).toMap().values.toList()
                    }
                }
            }
        }

        PeachButton(
            text = stringResource(if (needsPlan) R.string.items_place_plus else R.string.items_place),
            enabled = ui != null && (changed || needsPlan),
            loading = saving,
            onClick = {
                if (needsPlan) {
                    plus?.openPlans?.invoke(PaywallSource.CAT_ITEMS)
                } else {
                    saving = true
                    vm.saveItems(selection)
                }
            },
        )
    }
}

/** The cat with the chosen items standing around it, and "Seçili: …" below (board P7a, top card). */
@Composable
private fun ItemsPreview(selection: CatItems, catName: String) {
    val shape = RoundedCornerShape(28.dp)
    val names = selection.all.map { stringResource(it.nameRes) }
    val list = ListFormatter.getInstance(AppLocale.current()).format(names)
    val label = stringResource(R.string.items_preview_label, catName)
    Column(Modifier.fillMaxWidth().softShadow(shape, 14.dp).clip(shape).background(CatBg)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val height = CatCardGeometry.videoHeightDp(maxWidth.value).dp
            Box(Modifier.fillMaxWidth().height(height).clearAndSetSemantics { contentDescription = label }) {
                Image(
                    painterResource(R.drawable.cat_base),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                CatItemsLayer(selection, Modifier.fillMaxSize())
            }
        }
        Text(
            stringResource(R.string.items_selected, list),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        )
    }
}

/** One care action: its name and Plus bonus on the left, its models as tiles. */
@Composable
private fun ItemRow(slot: CareSlot, chosen: CatItem, showLocks: Boolean, tile: Dp, onPick: (CatItem) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
        Column(Modifier.width(LABEL_W.dp)) {
            Text(stringResource(slot.labelRes), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, lineHeight = 17.sp)
            Text(slot.effectText(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownMuted, lineHeight = 15.sp)
            Text(
                stringResource(R.string.items_plus_effect_note),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, lineHeight = 15.sp,
            )
        }
        CatItem.forSlot(slot).forEach { item ->
            ItemTile(item, selected = item == chosen, showLock = showLocks && item.plus, size = tile) { onPick(item) }
        }
    }
}

/** A 58dp model tile: picture, a dark check when chosen (peach border), a small lock on Plus models. */
@Composable
private fun ItemTile(item: CatItem, selected: Boolean, showLock: Boolean, size: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (selected) PeachTint else CreamCard, label = "tile")
    val name = stringResource(item.nameRes)
    val description = stringResource(if (item.plus) R.string.items_tile_plus else R.string.items_tile_free, name)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.5.dp else 1.5.dp, if (selected) PeachDeep else Outline, shape)
            .bounceClick(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(item.drawable), contentDescription = null, modifier = Modifier.size(size * (50f / TILE_MAX)))
        if (selected) {
            Box(
                Modifier.align(Alignment.TopStart).padding(3.dp).size(16.dp).clip(CircleShape).background(Brown),
                contentAlignment = Alignment.Center,
            ) { SvgStrokeIcon(Kit.CHECK_D, Cream, Modifier.size(10.dp), strokeWidth = 3f) }
        }
        if (showLock) {
            SvgStrokeIcon(Kit.LOCK_D, BrownSoft, Modifier.align(Alignment.TopEnd).padding(3.dp).size(12.dp), strokeWidth = 2.4f)
        }
    }
}

private val CareSlot.labelRes: Int
    get() = when (this) {
        CareSlot.Feed -> R.string.cat_feed
        CareSlot.Water -> R.string.cat_water
        CareSlot.Play -> R.string.cat_play
        CareSlot.Sleep -> R.string.cat_sleep
    }

/** "+10 tokluk", "15 dk erken": the numbers come from CatItemRules, never from the sentence. */
@Composable
private fun CareSlot.effectText(): String = when (this) {
    CareSlot.Feed -> CatItemRules.FEED_BONUS.toInt().let { pluralStringResource(R.plurals.items_effect_feed, it, it) }
    CareSlot.Water -> CatItemRules.WATER_BONUS.toInt().let { pluralStringResource(R.plurals.items_effect_water, it, it) }
    CareSlot.Play -> CatItemRules.PLAY_LOVE_BONUS.toInt().let { pluralStringResource(R.plurals.items_effect_play, it, it) }
    CareSlot.Sleep -> (CatItemRules.FREE_SLEEP_REFILL_MIN - CatItemRules.PLUS_SLEEP_REFILL_MIN)
        .let { pluralStringResource(R.plurals.items_effect_sleep, it, it) }
}
