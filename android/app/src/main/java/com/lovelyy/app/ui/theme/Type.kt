package com.lovelyy.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R

val Quicksand = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int) = TextStyle(
    fontFamily = Quicksand,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

val LovelyyTypography = Typography(
    displayLarge = style(48, FontWeight.Bold, 56),
    displayMedium = style(40, FontWeight.Bold, 48),
    displaySmall = style(34, FontWeight.Bold, 42),
    headlineLarge = style(30, FontWeight.Bold, 38),
    headlineMedium = style(26, FontWeight.Bold, 34),
    headlineSmall = style(22, FontWeight.SemiBold, 30),
    titleLarge = style(20, FontWeight.Bold, 28),
    titleMedium = style(17, FontWeight.SemiBold, 24),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Medium, 24),
    bodyMedium = style(14, FontWeight.Medium, 20),
    bodySmall = style(12, FontWeight.Medium, 16),
    labelLarge = style(16, FontWeight.Bold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 16),
    labelSmall = style(11, FontWeight.SemiBold, 14),
)
