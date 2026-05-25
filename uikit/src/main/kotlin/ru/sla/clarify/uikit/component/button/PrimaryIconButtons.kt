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
fun PrimaryIconButton(
  icon: Painter,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.primaryButtonColors()
) {
  IconButtonInternal(
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
fun PrimaryIconButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  colors: ButtonColors = ButtonDefaultsInternal.primaryButtonColors(),
  trailingIcon: @Composable () -> Unit
) {
  IconButtonInternal(
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
fun PrimaryIconButtonLarge(
  icon: Painter,
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false
) {
  IconButtonInternal(
    modifier = modifier,
    onClick = onClick,
    text = text,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = ButtonDefaultsInternal.primaryButtonColors(),
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    }
  ) {
    Icon(
      modifier = Modifier.size(24.dp),
      painter = icon,
      contentDescription = null
    )
  }
}

@Composable
fun PrimaryIconButtonLarge(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevated: Boolean = false,
  icon: @Composable () -> Unit
) {
  IconButtonInternal(
    modifier = modifier,
    icon = icon,
    text = text,
    enabled = enabled,
    size = ButtonSize.Large,
    colors = ButtonDefaultsInternal.primaryButtonColors(),
    elevation = if (elevated) {
      ButtonDefaultsInternal.elevation()
    } else {
      ButtonDefaultsInternal.defaultElevation()
    },
    onClick = onClick
  )
}

// region Previews

/** Состояние icon-кнопки для превью: enabled + elevation. */
private data class PrimaryIconButtonPreviewState(
  val label: String,
  val enabled: Boolean
)

private class PrimaryIconButtonPreviewStateProvider : PreviewParameterProvider<PrimaryIconButtonPreviewState> {
  override val values = sequenceOf(
    PrimaryIconButtonPreviewState(
      label = "enabled",
      enabled = true
    ),
    PrimaryIconButtonPreviewState(
      label = "disabled",
      enabled = false
    )
  )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Composable
private fun PrimaryIconButtonsPreviewLight(
  @PreviewParameter(PrimaryIconButtonPreviewStateProvider::class) state: PrimaryIconButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    PrimaryIconButtonsPreviewContent(state)
  }
}

@Preview(name = "Dark", showBackground = true, widthDp = 360)
@Composable
private fun PrimaryIconButtonsPreviewDark(
  @PreviewParameter(PrimaryIconButtonPreviewStateProvider::class) state: PrimaryIconButtonPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    PrimaryIconButtonsPreviewContent(state)
  }
}

@Composable
private fun PrimaryIconButtonsPreviewContent(state: PrimaryIconButtonPreviewState) {
  Column(
    modifier = Modifier
      .background(AppTheme.colors.surface)
      .padding(start = 16.dp, end = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(8.dp)
    PrimaryIconButton(
      modifier = Modifier.fillMaxWidth(),
      text = "Продолжить",
      onClick = {},
      icon = painterResource(R.drawable.ic_back_24),
      enabled = state.enabled,
      elevated = false
    )
    VSpacer(8.dp)
    PrimaryIconButtonLarge(
      modifier = Modifier.fillMaxWidth(),
      text = "Scroll down and read",
      onClick = {},
      icon = painterResource(R.drawable.ic_back_24),
      enabled = state.enabled,
      elevated = false
    )
    VSpacer(8.dp)
  }
}

// endregion
