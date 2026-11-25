package com.lovelyy.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Peach,
    onPrimary = Brown,
    primaryContainer = PeachSoft,
    onPrimaryContainer = Brown,
    secondary = Mint,
    onSecondary = Brown,
    secondaryContainer = MintSoft,
    onSecondaryContainer = Brown,
    tertiary = PeachDeep,
    background = Cream,
    onBackground = Brown,
    surface = Cream,
    onSurface = Brown,
    surfaceVariant = PeachSoft,
    onSurfaceVariant = BrownSoft,
    surfaceContainerLowest = CreamCard,
    surfaceContainerLow = CreamCard,
    surfaceContainer = CreamCard,
    surfaceContainerHigh = CreamCard,
    outline = Outline,
    outlineVariant = Outline,
    error = ErrorRose,
)

// v1 is light-only. To add dark mode later: define DarkColors here and pick
// it with isSystemInDarkTheme(). Screens must only use MaterialTheme.colorScheme
// (never raw colors) so this switch is all that's needed.
@Composable
fun LovelyyTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = LovelyyTypography,
        shapes = LovelyyShapes,
        content = content,
    )
}
