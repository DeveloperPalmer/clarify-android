package ru.sla.clarify.uikit.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class ColorTheme {
  Light,
  Dark
}

internal val LightColors = AppColors(
  isLight = true,
  primary = ColorPalette.primary40,
  onPrimary = ColorPalette.white,
  primaryContainer = ColorPalette.primary90,
  onPrimaryContainer = ColorPalette.primary10,
  secondary = ColorPalette.secondary40,
  onSecondary = ColorPalette.white,
  secondaryContainer = ColorPalette.secondary90,
  onSecondaryContainer = ColorPalette.secondary10,
  tertiary = ColorPalette.tertiary40,
  onTertiary = ColorPalette.white,
  tertiaryContainer = ColorPalette.tertiary90,
  onTertiaryContainer = ColorPalette.tertiary10,
  error = ColorPalette.error40,
  onError = ColorPalette.white,
  errorContainer = ColorPalette.error90,
  onErrorContainer = ColorPalette.error10,
  surfaceContainerLowest = ColorPalette.white,
  surfaceContainerLow = ColorPalette.neutral80,
  surfaceContainer = ColorPalette.neutral99,
  surfaceContainerHigh = ColorPalette.neutral95,
  surfaceContainerHighest = ColorPalette.neutral90,
  surfaceBright = ColorPalette.neutral70,
  surfaceDim = ColorPalette.neutral85,
  outline = ColorPalette.neutralVariant50,
  outlineVariant = ColorPalette.neutralVariant80,
  inversePrimary = ColorPalette.primary80,
  surface = ColorPalette.neutral70,
  onSurface = ColorPalette.neutral10,
  surfaceVariant = ColorPalette.neutralVariant90,
  onSurfaceVariant = ColorPalette.neutralVariant30
)

internal val DarkColors = AppColors(
  isLight = true,
  primary = ColorPalette.primary80,
  onPrimary = ColorPalette.primary20,
  primaryContainer = ColorPalette.primary30,
  onPrimaryContainer = ColorPalette.primary90,
  secondary = ColorPalette.secondary80,
  onSecondary = ColorPalette.secondary20,
  secondaryContainer = ColorPalette.secondary30,
  onSecondaryContainer = ColorPalette.secondary90,
  tertiary = ColorPalette.tertiary80,
  onTertiary = ColorPalette.tertiary20,
  tertiaryContainer = ColorPalette.tertiary90,
  onTertiaryContainer = ColorPalette.tertiary30,
  error = ColorPalette.error80,
  onError = ColorPalette.error20,
  errorContainer = ColorPalette.error30,
  onErrorContainer = ColorPalette.error90,
  surfaceContainerLowest = ColorPalette.neutral5,
  surfaceContainerLow = ColorPalette.neutral15,
  surfaceContainer = ColorPalette.neutral20,
  surfaceContainerHigh = ColorPalette.neutral25,
  surfaceContainerHighest = ColorPalette.neutral30,
  surfaceBright = ColorPalette.neutral35,
  surfaceDim = ColorPalette.neutral10,
  outline = ColorPalette.neutralVariant60,
  outlineVariant = ColorPalette.neutralVariant30,
  inversePrimary = ColorPalette.primary40,
  surface = ColorPalette.neutral10,
  onSurface = ColorPalette.neutral90,
  surfaceVariant = ColorPalette.neutralVariant30,
  onSurfaceVariant = ColorPalette.neutralVariant80
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
  error("No AppColors provided")
}
