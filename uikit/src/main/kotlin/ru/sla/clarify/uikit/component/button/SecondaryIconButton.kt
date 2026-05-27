package ru.sla.clarify.uikit.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun SecondaryIconButton(
  icon: Painter,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.secondaryButtonColors()
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    text = text,
    enabled = enabled,
    size = ButtonSize.Medium,
    colors = colors,
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    },
    onClick = onClick
  ) {
    Icon(
      modifier = Modifier.size(24.dp),
      painter = icon,
      contentDescription = null
    )
  }
}

@Composable
fun SecondaryIconButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.secondaryButtonColors(),
  trailingIcon: @Composable () -> Unit
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    icon = trailingIcon,
    text = text,
    enabled = enabled,
    size = ButtonSize.Medium,
    colors = colors,
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    },
    onClick = onClick
  )
}

@Composable
fun SecondaryIconButtonLarge(
  icon: Painter,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.secondaryButtonColors()
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    text = text,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = colors,
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    },
    onClick = onClick
  ) {
    Icon(
      modifier = Modifier.size(24.dp),
      painter = icon,
      contentDescription = null
    )
  }
}

@Composable
fun SecondaryIconButtonLarge(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.secondaryButtonColors(),
  trailingIcon: @Composable () -> Unit
) {
  OutlinedIconButtonInternal(
    modifier = modifier,
    icon = trailingIcon,
    text = text,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = colors,
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    },
    onClick = onClick
  )
}

/** Состояние icon-кнопки для превью: enabled + elevation. */
private data class SecondaryIconButtonPreviewState(
  val label: String,
  val enabled: Boolean,
  val elevated: Boolean
)

private class SecondaryIconButtonPreviewStateProvider : PreviewParameterProvider<SecondaryIconButtonPreviewState> {
  override val values = sequenceOf(
    SecondaryIconButtonPreviewState(
      label = "default",
      enabled = true,
      elevated = true
    ),
    SecondaryIconButtonPreviewState(
      label = "flat",
      enabled = true,
      elevated = false
    ),
    SecondaryIconButtonPreviewState(
      label = "disabled",
      enabled = false,
      elevated = true
    )
  )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Composable
private fun SecondaryIconButtonsPreviewLight(
  @PreviewParameter(SecondaryIconButtonPreviewStateProvider::class) state: SecondaryIconButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    SecondaryIconButtonsPreviewContent(state)
  }
}

@Preview(name = "Dark", showBackground = true, widthDp = 360)
@Composable
private fun SecondaryIconButtonsPreviewDark(
  @PreviewParameter(SecondaryIconButtonPreviewStateProvider::class) state: SecondaryIconButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    SecondaryIconButtonsPreviewContent(state)
  }
}

@Composable
private fun SecondaryIconButtonsPreviewContent(state: SecondaryIconButtonPreviewState) {
  val icon = painterResource(R.drawable.ic_back_24)
  Column(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    SecondaryIconButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      icon = icon,
      enabled = state.enabled,
      elevated = state.elevated
    )
    VSpacer(8.dp)
    SecondaryIconButtonLarge(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      icon = icon,
      enabled = state.enabled,
      elevated = state.elevated
    )
    VSpacer(8.dp)
  }
}

// endregion
