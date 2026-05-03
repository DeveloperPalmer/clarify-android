package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
  val isLight: Boolean,
  val bgPrimary: Color,
  val textPrimary: Color,
  val textInvertPrimary: Color,
  val surfaceNegative: Color,
  val buttonPrimaryBlackPress: Color
)
