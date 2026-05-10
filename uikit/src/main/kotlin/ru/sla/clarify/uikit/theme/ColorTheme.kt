package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ColorTheme {
  Light,
  Dark
}

internal val LightColors = AppColors(
  isLight = true,
  backgroundPrimary = Color.White,
  backgroundSecondary = ColorPalette.backgroundSecondary,
  textPrimary = ColorPalette.textPrimary,
  errorPrimary = ColorPalette.errorPrimary,
  buttonPrimary = Color.Gray
)

internal val DarkColors = AppColors(
  isLight = false,
  backgroundPrimary = Color.White,
  backgroundSecondary = ColorPalette.backgroundSecondary,
  textPrimary = ColorPalette.textPrimary,
  errorPrimary = ColorPalette.errorPrimary,
  buttonPrimary = Color.Gray
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
  error("No AppColors provided")
}
