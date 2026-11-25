package com.lovelyy.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownHint
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.Quicksand

// Building blocks of the approved design (login + home canvas).
// Soft shadows, 16-28dp corners, Quicksand, one big peach action per screen.

private val ShadowBrown = Color(0xFF4A3B35)

/** A soft brown shadow like the design's `0 6px 18px rgba(74,59,53,.06)`. */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 10.dp, tint: Color = ShadowBrown): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = tint.copy(alpha = 0.10f), spotColor = tint.copy(alpha = 0.16f))

/** Tap with a tiny bouncy squish and a haptic tick, without the Material ripple. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    haptic: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) pressedScale else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )
    val feedback = LocalHapticFeedback.current
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            haptic?.let(feedback::performHapticFeedback)
            onClick()
        }
}

/** The one main action: 58dp, peach, rounded 20, glowing shadow. */
@Composable
fun PeachButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 58.dp,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val active = enabled && !loading
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (enabled) Modifier.shadow(14.dp, shape, ambientColor = Peach, spotColor = PeachDeep) else Modifier)
            .clip(shape)
            .background(if (enabled) Peach else Color(0xFFF6E3D6))
            .alpha(if (enabled) 1f else 0.8f)
            .bounceClick(enabled = active, pressedScale = 0.97f, haptic = HapticFeedbackType.LongPress, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.5.dp, color = Brown, modifier = Modifier.size(22.dp))
        } else {
            leading?.let {
                it()
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = Brown, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

/** White button with a brown outline (secondary action). */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(CreamCard)
            .border(1.5.dp, Brown, shape)
            .alpha(if (enabled) 1f else 0.5f)
            .bounceClick(enabled = enabled && !loading, pressedScale = 0.97f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(strokeWidth = 2.5.dp, color = Brown, modifier = Modifier.size(22.dp))
        else Text(text, color = Brown, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** A plain text action ("Zaten hesabım var · Giriş yap"). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Brown, content: (@Composable () -> Unit)? = null) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content()
        else Text(text, color = color, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** 44dp white circle with an icon (back, bell...). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = CreamCard,
    tint: Color = Brown,
    size: Dp = 44.dp,
    badge: Boolean = false,
) {
    Box(
        modifier
            .size(size)
            .softShadow(CircleShape, 4.dp)
            .clip(CircleShape)
            .background(background)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 11.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8A5B))
                    .border(2.dp, CreamCard, CircleShape)
            )
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) =
    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Geri", onClick, modifier)

/** A white rounded card with a soft shadow. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = CreamCard,
    radius: Dp = 24.dp,
    shadow: Boolean = color == CreamCard,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .then(if (shadow) Modifier.softShadow(shape) else Modifier)
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Round tinted badge with an icon inside. */
@Composable
fun IconCircle(icon: ImageVector, background: Color, modifier: Modifier = Modifier, size: Dp = 40.dp, tint: Color = Brown, iconSize: Dp = 22.dp) {
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Text input from the design: 56dp, white, 18dp corners, icon on the left. */
@Composable
fun DesignField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(PeachDeep),
        textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Brown),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(shape)
                    .background(CreamCard)
                    .border(1.5.dp, if (focused) Peach else Outline, shape)
                    .padding(start = 16.dp, end = if (trailing != null) 6.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = BrownHint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFF9A8A83), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** Small rounded label ("Yakında", "Son adım"). */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, background: Color = CreamCard, color: Color = Brown, fontSize: Int = 13) {
    Text(
        text,
        color = color,
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Screen title row used by inner screens: back button + title. */
@Composable
fun ScreenTitle(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Spacer(Modifier.width(14.dp))
        Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Brown, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

/** Inner-screen top bar from the feature designs: round back button, centered 18sp title, round action. */
@Composable
fun CenteredTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Box(modifier.fillMaxWidth().height(44.dp)) {
        BackButton(onBack, Modifier.align(Alignment.CenterStart))
        Text(
            title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(Modifier.align(Alignment.CenterEnd)) { action?.invoke() }
    }
}
