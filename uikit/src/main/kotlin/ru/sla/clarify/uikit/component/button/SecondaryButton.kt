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
fun SecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  showLoading: Boolean = false
) {
  OutlinedButtonInternal(
    modifier = modifier,
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Medium,
    colors = ButtonDefaultsInternal.secondaryButtonColors(),
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    }
  )
}

@Composable
fun SecondaryButtonLarge(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  showLoading: Boolean = false
) {
  OutlinedButtonInternal(
    modifier = modifier,
    onClick = onClick,
    text = text,
    showLoading = showLoading,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = ButtonDefaultsInternal.secondaryButtonColors(),
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    }
  )
}

// region Previews

/** Состояние кнопки для превью: loading + enabled. */
private data class SecondaryButtonPreviewState(
  val label: String,
  val showLoading: Boolean,
  val enabled: Boolean
)

private class SecondaryButtonPreviewStateProvider : PreviewParameterProvider<SecondaryButtonPreviewState> {
  override val values = sequenceOf(
    SecondaryButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = false
    ),
    SecondaryButtonPreviewState(
      label = "loading",
      enabled = true,
      showLoading = true
    ),
    SecondaryButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = false
    ),
    SecondaryButtonPreviewState(
      label = "loading + disabled",
      enabled = false,
      showLoading = true
    )
  )
}

@Preview
@Composable
private fun SecondaryButtonsPreviewLight(
  @PreviewParameter(SecondaryButtonPreviewStateProvider::class)
  state: SecondaryButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    SecondaryButtonsPreviewContent(state)
  }
}

@Preview
@Composable
private fun SecondaryButtonsPreviewDark(
  @PreviewParameter(SecondaryButtonPreviewStateProvider::class)
  state: SecondaryButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    SecondaryButtonsPreviewContent(state)
  }
}

@Composable
private fun SecondaryButtonsPreviewContent(state: SecondaryButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.surface)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    SecondaryButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled
    )
    VSpacer(8.dp)
    SecondaryButtonLarge(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      showLoading = state.showLoading,
      enabled = state.enabled
    )
    VSpacer(8.dp)
  }
}

// endregion
