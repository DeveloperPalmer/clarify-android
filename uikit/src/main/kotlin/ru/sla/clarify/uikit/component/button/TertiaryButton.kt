package ru.sla.clarify.uikit.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun TertiaryButtonSmall(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  OutlinedButtonInternal(
    modifier = modifier,
    size = ButtonSize.Small,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.tertiaryDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.tertiaryDefaultColors()
      ButtonStyle.Success -> ButtonDefaults.tertiaryDefaultColors()
    },
    onClick = onClick
  )
}

@Composable
fun TertiaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  OutlinedButtonInternal(
    modifier = modifier,
    size = ButtonSize.Medium,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.tertiaryDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.tertiaryDefaultColors()
      ButtonStyle.Success -> ButtonDefaults.tertiaryDefaultColors()
    },
    onClick = onClick
  )
}

@Preview
@Composable
private fun TertiaryButtonsPreviewLight(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    TertiaryButtonsPreviewContent(state)
  }
}

@Preview
@Composable
private fun TertiaryButtonsPreviewDark(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    TertiaryButtonsPreviewContent(state)
  }
}

@Composable
private fun TertiaryButtonsPreviewContent(state: ButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    TertiaryButtonSmall(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      style = state.style,
      enabled = state.enabled,
      showLoading = state.showLoading,
      showElevation = state.showElevation,
      onClick = {}
    )
    VSpacer(8.dp)
    TertiaryButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      style = state.style,
      enabled = state.enabled,
      showLoading = state.showLoading,
      showElevation = state.showElevation,
      onClick = {}
    )
    VSpacer(8.dp)
  }
}
