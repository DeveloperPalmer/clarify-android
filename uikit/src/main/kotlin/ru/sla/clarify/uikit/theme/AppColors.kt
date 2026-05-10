package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
  val isLight: Boolean,

  val backgroundPrimary: Color,
  val backgroundSecondary: Color,
  val textPrimary: Color,
  val errorPrimary: Color,
  val buttonPrimary: Color
)
