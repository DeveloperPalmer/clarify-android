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
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun TextButtonSmall(
  text: String,
  isError: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  TextButtonInternal(
    modifier = modifier,
    size = ButtonSize.Small,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    colors = ButtonDefaults.textButtonColors(isError),
    onClick = onClick
  )
}

@Composable
fun TextButton(
  text: String,
  isError: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  TextButtonInternal(
    modifier = modifier,
    size = ButtonSize.Medium,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    colors = ButtonDefaults.textButtonColors(isError),
    onClick = onClick
  )
}

@Preview
@Composable
private fun TextButtonsPreviewLight(
  @PreviewParameter(TextButtonPreviewStateProvider::class)
  state: TextButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    TextButtonsPreviewContent(state)
  }
}

@Preview
@Composable
private fun TextButtonsPreviewDark(
  @PreviewParameter(TextButtonPreviewStateProvider::class)
  state: TextButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    TextButtonsPreviewContent(state)
  }
}

@Composable
private fun TextButtonsPreviewContent(state: TextButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    TextButtonSmall(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      isError = state.isError,
      enabled = state.enabled,
      showLoading = state.showLoading,
      onClick = {}
    )
    VSpacer(8.dp)
    TextButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      isError = state.isError,
      enabled = state.enabled,
      showLoading = state.showLoading,
      onClick = {}
    )
    VSpacer(8.dp)
  }
}

internal data class TextButtonPreviewState(
  val label: String,
  val isError: Boolean,
  val enabled: Boolean,
  val showLoading: Boolean
)

internal class TextButtonPreviewStateProvider : PreviewParameterProvider<TextButtonPreviewState> {
  override val values = sequenceOf(
    TextButtonPreviewState(
      label = "default",
      isError = true,
      enabled = true,
      showLoading = false
    ),
    TextButtonPreviewState(
      label = "default",
      isError = true,
      enabled = true,
      showLoading = true
    ),
    TextButtonPreviewState(
      label = "disabled",
      isError = true,
      enabled = false,
      showLoading = false
    ),
    TextButtonPreviewState(
      label = "disabled",
      isError = true,
      enabled = false,
      showLoading = true
    ),
    TextButtonPreviewState(
      label = "elevation",
      isError = true,
      enabled = true,
      showLoading = false
    )
  )
}
