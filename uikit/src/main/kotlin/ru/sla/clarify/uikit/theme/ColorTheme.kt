package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ColorTheme {
  Light,
  Dark
}

internal val LightColors = AppColors(
  isLight = true,
  bgPrimary = Color.White,
  textPrimary = Color.Black,
  textInvertPrimary = Color.Black,
  surfaceNegative = Color.Red,
  buttonPrimaryBlackPress = Color.Gray
)

internal val DarkColors = AppColors(
  isLight = false,
  bgPrimary = Color.White,
  textPrimary = Color.Black,
  textInvertPrimary = Color.Black,
  surfaceNegative = Color.Red,
  buttonPrimaryBlackPress = Color.Gray
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
  error("No AppColors provided")
}
