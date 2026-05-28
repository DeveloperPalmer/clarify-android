package ru.sla.clarify.uikit.component.button

import androidx.annotation.DrawableRes
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
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun TertiaryIconButton(
  @DrawableRes
  iconRes: Int,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  IconButtonInternal(
    modifier = modifier,
    iconRes = iconRes,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    size = ButtonSize.Medium,
    colors = ButtonDefaults.tertiaryButtonColors(),
    onClick = onClick
  )
}

@Composable
fun TertiaryIconButtonLarge(
  @DrawableRes
  iconRes: Int,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  IconButtonInternal(
    iconRes = iconRes,
    modifier = modifier,
    onClick = onClick,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    size = ButtonSize.Large,
    colors = ButtonDefaults.tertiaryButtonColors()
  )
}

@Preview
@Composable
private fun TertiaryIconButtonsPreviewLight(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    TertiaryIconButtonsPreviewContent(state)
  }
}

@Preview
@Composable
private fun TertiaryIconButtonsPreviewDark(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    TertiaryIconButtonsPreviewContent(state)
  }
}

@Composable
private fun TertiaryIconButtonsPreviewContent(state: ButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    TertiaryIconButton(
      modifier = Modifier.fillMaxWidth(),
      iconRes = R.drawable.ic_back_24,
      text = "Продолжить",
      onClick = {},
      enabled = state.enabled,
      showLoading = state.showLoading
    )
    VSpacer(8.dp)
    TertiaryIconButtonLarge(
      modifier = Modifier.fillMaxWidth(),
      iconRes = R.drawable.ic_back_24,
      text = "Scroll down and read",
      onClick = {},
      enabled = state.enabled,
      showLoading = state.showLoading
    )
    VSpacer(8.dp)
  }
}
