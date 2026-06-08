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
fun SecondaryIconButtonSmall(
  @DrawableRes
  iconRes: Int,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    size = ButtonSize.Small,
    iconRes = iconRes,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.primaryDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.primaryErrorColors()
      ButtonStyle.Success -> ButtonDefaults.primarySuccessColors()
    },
    onClick = onClick
  )
}

@Composable
fun SecondaryIconButton(
  @DrawableRes
  iconRes: Int,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ButtonStyle = ButtonStyle.Default,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    size = ButtonSize.Medium,
    iconRes = iconRes,
    text = text,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = when (style) {
      ButtonStyle.Default -> ButtonDefaults.primaryDefaultColors()
      ButtonStyle.Error -> ButtonDefaults.primaryErrorColors()
      ButtonStyle.Success -> ButtonDefaults.primarySuccessColors()
    },
    onClick = onClick
  )
}

@Preview
@Composable
private fun SecondaryIconButtonsPreviewLight(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    SecondaryIconButtonsPreviewContent(state)
  }
}

@Preview
@Composable
private fun SecondaryIconButtonsPreviewDark(
  @PreviewParameter(ButtonPreviewStateProvider::class)
  state: ButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    SecondaryIconButtonsPreviewContent(state)
  }
}

@Composable
private fun SecondaryIconButtonsPreviewContent(state: ButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    SecondaryIconButtonSmall(
      modifier = Modifier.fillMaxWidth(),
      iconRes = R.drawable.ic_back_24,
      text = "Продолжить",
      style = state.style,
      enabled = state.enabled,
      showLoading = state.showLoading,
      showElevation = state.showElevation,
      onClick = {}
    )
    VSpacer(8.dp)
    SecondaryIconButton(
      modifier = Modifier.fillMaxWidth(),
      iconRes = R.drawable.ic_back_24,
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
