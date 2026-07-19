package ru.sla.clarify.uikit.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

object AppTheme {
  val colors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

  val elevation: AppElevation
    @Composable
    @ReadOnlyComposable
    get() = LocalAppElevation.current

  val typography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current

  val shapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalAppShapes.current

  val motion: AppMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalAppMotion.current
}

@Composable
fun AppTheme(
  currentTheme: ColorTheme,
  content: @Composable () -> Unit
) {
  val colors = remember(currentTheme) {
    when (currentTheme) {
      ColorTheme.Light -> LightColors
      ColorTheme.Dark -> DarkColors
    }
  }
  val shapes = remember { AppShapes() }
  val motion = remember { AppMotion() }
  val elevation = remember { AppElevation() }
  val typography = remember { AppTypography() }

  // Каретка и её ручка — акцентно-фиолетовые; фон выделенного текста совпадает с фоном
  // выделения коммита в режиме selection (backgroundAccentPrimary).
  val textSelectionColors = TextSelectionColors(
    handleColor = colors.contentAccentPrimary,
    backgroundColor = colors.backgroundAccentPrimary
  )
  CompositionLocalProvider(
    LocalAppColors provides colors,
    LocalAppShapes provides shapes,
    LocalAppMotion provides motion,
    LocalAppElevation provides elevation,
    LocalAppTypography provides typography,
    LocalContentColor provides colors.cardPrimary,
    LocalTextSelectionColors provides textSelectionColors,
    content = content
  )
}
