package com.lovelyy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.Quicksand

/** Shared pieces of the approved designs (boards 12-19). */
object Kit {
    val ButtonIdle = Color(0xFFF6EEE8)
    val Green = Color(0xFF3E9C82)
    val MintCell = Color(0xFFD6F0E8)
    val Butter = Color(0xFFFFE2A8)
    val CellLabel = Color(0xFF5F524C)
    val Dashed = Color(0xFFB8A69D)
    val FieldBorder = Color(0xFFEADFD6)

    const val PLUS_D = "M12 5v14M5 12h14"
    const val SEARCH_D = "M17.5 11a6.5 6.5 0 1 1-13 0a6.5 6.5 0 1 1 13 0z M16 16l4 4"
    const val LOCK_D = "M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 17.5v-4A2.5 2.5 0 0 1 7.5 11z M8 11V8a4 4 0 0 1 8 0v3"
    const val ENVELOPE_D = "M6 6h12a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z M4 8l8 6 8-6"
    const val PENCIL_D = "M4 20l4-1 11-11-3-3L5 16z"
    const val PHOTO_D = "M6 5h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z M11 10a2 2 0 1 1-4 0a2 2 0 1 1 4 0z M21 16l-5-5-8 8"
    const val CHECK_D = "M5 12.5l4.5 4.5L19 7.5"
    const val X_D = "M7 7l10 10M17 7L7 17"
}

/** The big peach action at the bottom of a screen (56dp, icon + label). */
@Composable
fun MainAction(text: String, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .softShadow(shape, 12.dp, Peach)
            .clip(shape)
            .background(if (enabled) Peach else Peach.copy(alpha = 0.5f))
            .semantics { role = Role.Button }
            .bounceClick(enabled = enabled, pressedScale = 0.97f, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgStrokeIcon(icon, Brown, Modifier.size(20.dp), strokeWidth = 2.4f)
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, modifier = Modifier.padding(start = 10.dp))
    }
}

/** White rounded card from the designs (24dp corners, soft shadow). */
@Composable
fun WhiteCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, radius: Dp = 24.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(CreamCard)
            .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Round initial avatar ("U", "E"). */
@Composable
fun InitialAvatar(name: String, color: Color, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier, border: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(if (border) Modifier.border(3.dp, Cream, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = fontSize, color = Brown)
    }
}

/** Two overlapping avatars (me peach, partner mint). */
@Composable
fun CoupleAvatars(me: String, partner: String, size: Dp = 42.dp, overlap: Dp = 14.dp, fontSize: TextUnit = 16.sp) {
    Box(Modifier.size(width = size * 2 - overlap, height = size)) {
        InitialAvatar(me, Peach, size, fontSize)
        InitialAvatar(partner, Mint, size, fontSize, Modifier.offset(x = size - overlap))
    }
}

/** Small rounded label (chip) like "10 gün" or "Oku". */
@Composable
fun TagChip(text: String, bg: Color, modifier: Modifier = Modifier) {
    Text(
        text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1, softWrap = false,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/** Header used by the tabs: avatars, title + subtitle, one round action. */
@Composable
fun TabHeader(me: String, partner: String, title: String, subtitle: String, action: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CoupleAvatars(me, partner)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
            Text(subtitle, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
        }
        action()
    }
}

/** A row with title, subtitle and the design's 52×30 switch. */
@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            Text(subtitle, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
        }
        val track by animateColorAsState(if (checked) Kit.Green else Color(0xFFE3D8D0), label = "track")
        val x by animateDpAsState(if (checked) 25.dp else 3.dp, label = "knob")
        Box(Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track)) {
            Box(Modifier.offset(x = x, y = 3.dp).size(24.dp).softShadow(CircleShape, 2.dp).clip(CircleShape).background(CreamCard))
        }
    }
}

/** Text field for the bottom sheets (white, 18dp corners, peach border when focused). */
@Composable
fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        interactionSource = interaction,
        cursorBrush = SolidColor(PeachDeep),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = Brown),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(CreamCard)
                    .border(2.dp, if (focused) PeachDeep else Kit.FieldBorder, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(hint, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrownSoft)
                inner()
            }
        },
    )
}

/** Selectable pill used in sheets (category, wheel picker). */
@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Brown else CreamCard)
            .semantics { role = Role.Tab }
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = if (selected) Cream else Brown, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
        )
    }
}

/** "Emin misin?" before something is deleted or thrown away. [confirm] is red: the action can't be undone. */
@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown) },
        text = { Text(body, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft) },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color(0xFFA8401C)),
            ) { Text(confirm, fontFamily = Quicksand, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Brown),
            ) { Text(androidx.compose.ui.res.stringResource(com.lovelyy.app.R.string.action_cancel), fontFamily = Quicksand, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Cream,
    )
}
