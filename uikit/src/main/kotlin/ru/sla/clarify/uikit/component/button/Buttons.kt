package ru.sla.clarify.uikit.component.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.HSpacer

@Composable
internal fun ButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  elevation: ButtonElevation = ButtonDefaultsInternal.defaultElevation()
) {
  Button(
    modifier = modifier.heightIn(size.toDp()),
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      text = text,
      colors = colors,
      enabled = enabled,
      showLoading = showLoading
    )
  }
}

@Composable
internal fun OutlinedButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  elevation: ButtonElevation = ButtonDefaultsInternal.defaultElevation()
) {
  OutlinedButton(
    modifier = modifier.heightIn(size.toDp()),
    enabled = enabled,
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    colors = colors,
    elevation = elevation,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      text = text,
      colors = colors,
      enabled = enabled,
      showLoading = showLoading
    )
  }
}

@Composable
internal fun IconButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevation: ButtonElevation = ButtonDefaultsInternal.defaultElevation(),
  icon: @Composable () -> Unit
) {
  Button(
    modifier = modifier.heightIn(min = size.toDp()),
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = onClick
  ) {
    icon()
    HSpacer(16.dp)
    Text(
      text = text,
      style = AppTheme.typography.title3,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
internal fun OutlinedIconButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevation: ButtonElevation = ButtonDefaultsInternal.defaultElevation(),
  icon: @Composable () -> Unit
) {
  OutlinedButton(
    modifier = modifier.heightIn(min = size.toDp()),
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = onClick
  ) {
    icon()
    HSpacer(16.dp)
    Text(
      text = text,
      style = AppTheme.typography.title3,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
internal fun IconRightSideOutlinedButtonInternal(
  icon: Painter,
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  elevation: ButtonElevation = ButtonDefaultsInternal.defaultElevation()
) {
  OutlinedButton(
    modifier = modifier.heightIn(size.toDp()),
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    enabled = enabled,
    colors = colors,
    elevation = elevation,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = onClick
  ) {
    Text(
      modifier = Modifier.weight(weight = 1f),
      text = text,
      style = AppTheme.typography.title3,
      textAlign = TextAlign.Center
    )
    HSpacer(16.dp)
    Icon(
      modifier = Modifier.size(24.dp),
      painter = icon,
      contentDescription = null
    )
  }
}

@Composable
private fun RowScope.ButtonContent(
  text: String,
  enabled: Boolean,
  showLoading: Boolean,
  colors: ButtonColors
) {
  AnimatedVisibility(showLoading) {
    Row {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = colors.contentColor(enabled = enabled).value,
        strokeWidth = 2.dp
      )
      HSpacer(16.dp)
    }
  }
  Text(
    text = text,
    style = AppTheme.typography.title3
  )
}

private fun ButtonSize.toDp(): Dp {
  return when (this) {
    ButtonSize.Medium -> 40.dp
    ButtonSize.Large -> 56.dp
  }
}

internal object ButtonDefaultsInternal {
  @Composable
  fun primaryButtonColors(): ButtonColors {
    return ButtonColors(
      containerColor = AppTheme.colors.buttonPrimaryBg,
      contentColor = AppTheme.colors.buttonPrimaryContent,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonPrimaryContentDisabled
    )
  }

  @Composable
  fun secondaryButtonColors(): ButtonColors {
    return ButtonColors(
      containerColor = AppTheme.colors.buttonSecondaryBg,
      contentColor = AppTheme.colors.buttonSecondaryContent,
      disabledContainerColor = AppTheme.colors.buttonSecondaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonSecondaryContentDisabled
    )
  }

  @Composable
  fun defaultElevation(): ButtonElevation {
    return ButtonDefaults.buttonElevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp
    )
  }

  @Composable
  fun elevation(): ButtonElevation {
    return ButtonDefaults.buttonElevation(
      defaultElevation = 8.dp,
      pressedElevation = 12.dp
    )
  }
}

@Composable
private fun ButtonColors.contentColor(enabled: Boolean): State<Color> {
  return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
}

internal enum class ButtonSize {
  Large,
  Medium
}
