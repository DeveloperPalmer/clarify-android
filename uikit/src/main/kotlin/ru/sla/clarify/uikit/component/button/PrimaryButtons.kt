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
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  ButtonInternal(
    modifier = modifier,
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Medium,
    colors = ButtonDefaults.primaryButtonColors()
  )
}

@Composable
fun PrimaryButtonLarge(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  ButtonInternal(
    modifier = modifier,
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = ButtonDefaults.primaryButtonColors()
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
    PrimaryButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled
    )
    VSpacer(8.dp)
    PrimaryButtonLarge(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled
    )
    VSpacer(8.dp)
  }
}
