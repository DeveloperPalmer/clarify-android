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

  val typography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current

  val shapes: AppShapes = AppShapes
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
  val typography = remember { AppTypography() }

  val textSelectionColors = TextSelectionColors(
    handleColor = colors.surfaceContainer,
    backgroundColor = colors.onSurface
  )
  CompositionLocalProvider(
    LocalAppColors provides colors,
    LocalAppTypography provides typography,
    LocalContentColor provides colors.surfaceContainer,
    LocalTextSelectionColors provides textSelectionColors,
    content = content
  )
}
