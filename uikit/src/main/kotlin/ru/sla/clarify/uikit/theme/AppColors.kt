package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
  val isLight: Boolean,
  val primary: Color,
  val primaryContainer: Color,
  val onPrimary: Color,
  val onPrimaryContainer: Color,
  val secondary: Color,
  val secondaryContainer: Color,
  val onSecondary: Color,
  val onSecondaryContainer: Color,
  val tertiary: Color,
  val tertiaryContainer: Color,
  val onTertiary: Color,
  val onTertiaryContainer: Color,
  val error: Color,
  val errorContainer: Color,
  val onError: Color,
  val onErrorContainer: Color,
  val surfaceContainerLowest: Color,
  val surfaceContainerLow: Color,
  val surfaceContainer: Color,
  val surfaceContainerHigh: Color,
  val surfaceContainerHighest: Color,
  val surfaceBright: Color,
  val surfaceDim: Color,
  val surface: Color,
  val onSurface: Color,
  val onSurfaceVariant: Color,
  val surfaceVariant: Color,
  val outline: Color,
  val outlineVariant: Color,
  val inversePrimary: Color
)
