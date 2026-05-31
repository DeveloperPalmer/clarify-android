package ru.sla.clarify.uikit.component.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  Button(
    modifier = modifier,
    iconRes = null,
    text = text,
    size = size,
    shape = AppTheme.shapes.round16,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = colors,
    onClick = { if (!showLoading) onClick() }
  )
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
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  Button(
    modifier = modifier,
    iconRes = iconRes,
    text = text,
    size = size,
    shape = AppTheme.shapes.round16,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = colors,
    onClick = { if (!showLoading) onClick() }
  )
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
  showElevation: Boolean = false
) {
  OutlinedButton(
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    modifier = modifier,
    iconRes = null,
    text = text,
    size = size,
    shape = AppTheme.shapes.round16,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = colors,
    onClick = { if (!showLoading) onClick() }
  )
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
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  OutlinedButton(
    border = BorderStroke(Dp.Hairline, AppTheme.colors.cardSecondary),
    modifier = modifier,
    iconRes = iconRes,
    text = text,
    size = size,
    shape = AppTheme.shapes.round16,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = colors,
    onClick = { if (!showLoading) onClick() }
  )
}

@Composable
private fun OutlinedButton(
  @DrawableRes
  iconRes: Int?,
  text: String,
  border: BorderStroke?,
  shape: Shape,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false,
  interactionSource: MutableInteractionSource? = null
) {
  Button(
    modifier = modifier,
    iconRes = iconRes,
    text = text,
    size = size,
    shape = shape,
    border = border,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    colors = colors,
    interactionSource = interactionSource,
    onClick = onClick
  )
}

@Composable
internal fun TextButtonInternal(
  text: String,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false
) {
  Button(
    modifier = modifier,
    iconRes = null,
    text = text,
    size = size,
    enabled = enabled,
    showLoading = showLoading,
    showElevation = showElevation,
    shape = AppTheme.shapes.round16,
    colors = colors,
    onClick = { if (!showLoading) onClick() }
  )
}

@Composable
private fun Button(
  @DrawableRes
  iconRes: Int?,
  text: String,
  shape: Shape,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  showLoading: Boolean = false,
  showElevation: Boolean = false,
  border: BorderStroke? = null,
  interactionSource: MutableInteractionSource? = null
) {
  @Suppress("NAME_SHADOWING")
  val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
  val containerColor = colors.containerColor(enabled)
  val contentColor = colors.contentColor(enabled)
  val elevation = if (showElevation && containerColor.value.alpha > 0f) 2.dp else 0.dp
  Surface(
    onClick = onClick,
    modifier = modifier
      .heightIn(size.height())
      .semantics { role = Role.Button },
    shape = shape,
    border = border,
    enabled = enabled,
    color = containerColor.value,
    contentColor = contentColor.value,
    tonalElevation = elevation,
    shadowElevation = elevation,
    interactionSource = interactionSource
  ) {
    AnimatedContent(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
      targetState = showLoading,
      transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { loading ->
      Row(
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
            style = when (size) {
              ButtonSize.Small -> AppTheme.typography.label3
              ButtonSize.Medium -> AppTheme.typography.title3
            },
            color = colors.contentColor(enabled = enabled).value
          )
        } else {
          Text(
            text = text,
            style = when (size) {
              ButtonSize.Small -> AppTheme.typography.label3
              ButtonSize.Medium -> AppTheme.typography.title3
            },
            color = colors.contentColor(enabled = enabled).value
          )
        }
      }
    }
  }
}

private fun ButtonSize.height(): Dp {
  return when (this) {
    ButtonSize.Small -> 32.dp
    ButtonSize.Medium -> 48.dp
  }
}

private fun ButtonSize.iconSize(): Dp {
  return when (this) {
    ButtonSize.Small -> 16.dp
    ButtonSize.Medium -> 20.dp
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
  fun textButtonColors(isError: Boolean): ButtonColors {
    return ButtonDefaultsInternal.textButtonColors(
      contentColor = if (isError) {
        AppTheme.colors.errorPrimary
      } else {
        AppTheme.colors.buttonTertiaryContent
      },
      disabledContentColor = AppTheme.colors.buttonTertiaryContentDisabled
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

@Composable
private fun ButtonColors.containerColor(enabled: Boolean): State<Color> {
  return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
}

internal enum class ButtonSize {
  Small,
  Medium
}

internal data class ButtonPreviewState(
  val label: String,
  val enabled: Boolean,
  val showLoading: Boolean,
  val showElevation: Boolean
)

internal class ButtonPreviewStateProvider : PreviewParameterProvider<ButtonPreviewState> {
  override val values = sequenceOf(
    ButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = false,
      showElevation = false
    ),
    ButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = true,
      showElevation = false
    ),
    ButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = false,
      showElevation = false
    ),
    ButtonPreviewState(
      label = "disabled",
      enabled = false,
      showLoading = true,
      showElevation = false
    ),
    ButtonPreviewState(
      label = "default",
      enabled = true,
      showLoading = false,
      showElevation = true
    )
  )
}
