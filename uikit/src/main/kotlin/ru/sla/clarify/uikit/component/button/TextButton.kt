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
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Small,
    colors = ButtonDefaults.textButtonColors(isError)
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
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Medium,
    colors = ButtonDefaults.textButtonColors(isError)
  )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Composable
private fun TextButtonsPreviewLight(
  @PreviewParameter(TextButtonPreviewStateProvider::class)
  state: TextButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    TextButtonsPreviewContent(state)
  }
}

@Preview(name = "Dark", showBackground = true, widthDp = 360)
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
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled,
      isError = state.isError
    )
    VSpacer(8.dp)
    TextButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled,
      isError = state.isError
    )
    VSpacer(8.dp)
  }
}

internal data class TextButtonPreviewState(
  val label: String,
  val enabled: Boolean = true,
  val showLoading: Boolean = false,
  val isError: Boolean = false
)

internal class TextButtonPreviewStateProvider : PreviewParameterProvider<TextButtonPreviewState> {
  override val values = sequenceOf(
    TextButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = false,
      isError = true
    ),
    TextButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = true,
      isError = true
    ),
    TextButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = false,
      isError = true
    ),
    TextButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = true,
      isError = true
    )
  )
}
