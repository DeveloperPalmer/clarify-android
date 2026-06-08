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
fun PrimaryTextButtonSmall(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  TextButtonInternal(
    modifier = modifier,
    size = ButtonSize.Small,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.textDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.textErrorColors()
      ButtonStyle.Success -> ButtonDefaults.textSuccessColors()
    },
    onClick = onClick
  )
}

@Composable
fun PrimaryTextButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  TextButtonInternal(
    modifier = modifier,
    size = ButtonSize.Medium,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.textDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.textErrorColors()
      ButtonStyle.Success -> ButtonDefaults.textSuccessColors()
    },
    onClick = onClick
  )
}

@Preview
@Composable
private fun PrimaryTextButtonsPreviewLight(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  preview: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    PrimaryTextButtonsPreviewContent(preview)
  }
}

@Preview
@Composable
private fun PrimaryTextButtonsPreviewDark(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  preview: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    PrimaryTextButtonsPreviewContent(preview)
  }
}

@Composable
private fun PrimaryTextButtonsPreviewContent(preview: ButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    PrimaryTextButtonSmall(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      style = preview.style,
      enabled = preview.enabled,
      showLoading = preview.showLoading,
      onClick = {}
    )
    VSpacer(8.dp)
    PrimaryTextButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      style = preview.style,
      enabled = preview.enabled,
      showLoading = preview.showLoading,
      onClick = {}
    )
    VSpacer(8.dp)
  }
}
