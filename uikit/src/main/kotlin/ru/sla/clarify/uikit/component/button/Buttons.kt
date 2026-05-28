package ru.sla.clarify.uikit.component.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import androidx.compose.material3.ButtonDefaults as ButtonDefaultsInternal

@Composable
internal fun ButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  Button(
    modifier = modifier.heightIn(size.height()),
    enabled = enabled,
    colors = colors,
    elevation = null,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      iconRes = null,
      text = text,
      enabled = enabled,
      showLoading = showLoading,
      size = size,
      colors = colors
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
  showLoading: Boolean = false
) {
  OutlinedButton(
    modifier = modifier.heightIn(size.height()),
    enabled = enabled,
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    colors = colors,
    elevation = null,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      iconRes = null,
      text = text,
      enabled = enabled,
      showLoading = showLoading,
      size = size,
      colors = colors
    )
  }
}

@Composable
internal fun IconButtonInternal(
  @DrawableRes
  iconRes: Int?,
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  Button(
    modifier = modifier.heightIn(min = size.height()),
    enabled = enabled,
    colors = colors,
    elevation = null,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      iconRes = iconRes,
      text = text,
      enabled = enabled,
      showLoading = showLoading,
      size = size,
      colors = colors
    )
  }
}

@Composable
internal fun OutlinedIconButtonInternal(
  @DrawableRes
  iconRes: Int?,
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false
) {
  OutlinedButton(
    modifier = modifier.heightIn(min = size.height()),
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    enabled = enabled,
    colors = colors,
    elevation = null,
    shape = AppTheme.shapes.round16,
    contentPadding = PaddingValues(16.dp),
    onClick = { if (!showLoading) onClick() }
  ) {
    ButtonContent(
      iconRes = iconRes,
      text = text,
      enabled = enabled,
      showLoading = showLoading,
      size = size,
      colors = colors
    )
  }
}

@Composable
private fun ButtonContent(
  @DrawableRes
  iconRes: Int?,
  text: String,
  size: ButtonSize,
  enabled: Boolean,
  showLoading: Boolean,
  colors: ButtonColors
) {
  AnimatedContent(
    targetState = showLoading,
    transitionSpec = { fadeIn() togetherWith fadeOut() }
  ) { loading ->
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(
        space = 16.dp,
        alignment = Alignment.CenterHorizontally
      )
    ) {
      if (loading) {
        CircularProgressIndicator(
          modifier = Modifier.size(size.iconSize()),
          color = colors.contentColor(enabled = enabled).value,
          strokeWidth = 2.dp
        )
      } else if (iconRes != null) {
        Icon(
          modifier = Modifier.size(size.iconSize()),
          painter = painterResource(iconRes),
          contentDescription = null
        )
        Text(
          text = text,
          style = AppTheme.typography.title3,
          color = colors.contentColor(enabled = enabled).value
        )
      } else {
        Text(
          text = text,
          style = AppTheme.typography.title3,
          color = colors.contentColor(enabled = enabled).value
        )
      }
    }
  }
}

private fun ButtonSize.height(): Dp {
  return when (this) {
    ButtonSize.Medium -> 40.dp
    ButtonSize.Large -> 56.dp
  }
}

private fun ButtonSize.iconSize(): Dp {
  return when (this) {
    ButtonSize.Medium -> 16.dp
    ButtonSize.Large -> 20.dp
  }
}

internal object ButtonDefaults {
  @Composable
  fun primaryButtonColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonPrimaryBg,
      contentColor = AppTheme.colors.buttonPrimaryContent,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonPrimaryContentDisabled
    )
  }

  @Composable
  fun secondaryButtonColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonSecondaryBg,
      contentColor = AppTheme.colors.buttonSecondaryContent,
      disabledContainerColor = AppTheme.colors.buttonSecondaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonSecondaryContentDisabled
    )
  }

  @Composable
  fun tertiaryButtonColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.backgroundAccentPrimary,
      contentColor = AppTheme.colors.cardAccent,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.backgroundPrimary
    )
  }

  @Composable
  fun errorButtonColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.errorPrimary,
      contentColor = AppTheme.colors.backgroundPrimary,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.backgroundPrimary
    )
  }

  @Composable
  fun defaultElevation(): ButtonElevation {
    return ButtonDefaultsInternal.buttonElevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp
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

internal data class ButtonPreviewState(
  val label: String,
  val enabled: Boolean,
  val showLoading: Boolean
)

internal class ButtonPreviewStateProvider : PreviewParameterProvider<ButtonPreviewState> {
  override val values = sequenceOf(
    ButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = false
    ),
    ButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = true
    ),
    ButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = false
    ),
    ButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = true
    )
  )
}
