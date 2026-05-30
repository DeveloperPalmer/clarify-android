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
fun PrimaryButtonSmall(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  ButtonInternal(
    modifier = modifier,
    size = ButtonSize.Small,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = ButtonDefaults.primaryButtonColors(),
    onClick = onClick
  )
}

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  ButtonInternal(
    modifier = modifier,
    size = ButtonSize.Medium,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = ButtonDefaults.primaryButtonColors(),
    onClick = onClick
  )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Composable
private fun PrimaryButtonsPreviewLight(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    PrimaryButtonsPreviewContent(state)
  }
}

@Preview(name = "Dark", showBackground = true, widthDp = 360)
@Composable
private fun PrimaryButtonsPreviewDark(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    PrimaryButtonsPreviewContent(state)
  }
}

@Composable
private fun PrimaryButtonsPreviewContent(state: ButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    PrimaryButtonSmall(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      enabled = state.enabled,
      showLoading = state.showLoading,
      showElevation = state.showElevation,
      onClick = {}
    )
    VSpacer(8.dp)
    PrimaryButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      enabled = state.enabled,
      showLoading = state.showLoading,
      showElevation = state.showElevation,
      onClick = {}
    )
    VSpacer(8.dp)
  }
}
