package ru.kode.demo.uikit.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class ColorTheme {
  Light, Dark
}

internal val LightColors = AppColors(
  isLight = true,
  color1 = ColorPalette.Red,
  color2 = ColorPalette.Green,
  color3 = ColorPalette.Blue
)

internal val DarkColors = AppColors(
  isLight = false,
  color1 = ColorPalette.Blue,
  color2 = ColorPalette.Green,
  color3 = ColorPalette.Red
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
  error("No AppColors provided")
}
