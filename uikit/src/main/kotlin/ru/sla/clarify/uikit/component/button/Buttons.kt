package ru.sla.clarify.uikit.component.button

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme
import androidx.compose.material3.ButtonDefaults as ButtonDefaultsInternal

enum class ButtonStyle {
  Default,
  Error,
  Success
}

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
internal fun OutlinedTextButtonInternal(
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
private fun Button(
  @DrawableRes
  iconRes: Int?,
  text: String,
  shape: Shape,
  size: ButtonSize,
  colors: ButtonColors,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  textStyle: TextStyle = when (size) {
    ButtonSize.Small -> AppTheme.typography.body3Bold
    ButtonSize.Medium -> AppTheme.typography.title3
  },
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
    val contentAlpha by animateFloatAsState(
      targetValue = if (showLoading) 0f else 1f,
      label = "buttonContentAlpha"
    )
    Box(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
      contentAlignment = Alignment.Center
    ) {
      // Source content acts as the size anchor: it always occupies the button's
      // intrinsic width/height, so the button keeps its size while the loader is shown.
      Row(
        modifier = Modifier.alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (size) {
          ButtonSize.Small -> Arrangement.spacedBy(
            space = 6.dp,
            alignment = Alignment.CenterHorizontally
          )
          ButtonSize.Medium -> Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterHorizontally
          )
        }
      ) {
        if (iconRes != null) {
          Icon(
            modifier = Modifier.size(size.iconSize()),
            painter = painterResource(iconRes),
            contentDescription = null
          )
        }
        Text(
          text = text,
          style = textStyle,
          color = colors.contentColor(enabled = enabled).value
        )
      }
      AnimatedVisibility(
        visible = showLoading,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(size.iconSize()),
          color = colors.contentColor(enabled = enabled).value,
          strokeWidth = 2.dp
        )
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
  fun primaryDefaultColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonPrimaryBg,
      contentColor = AppTheme.colors.buttonPrimaryContent,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonPrimaryContentDisabled
    )
  }

  @Composable
  fun primaryErrorColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.errorPrimary,
      contentColor = AppTheme.colors.backgroundPrimary,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.backgroundPrimary
    )
  }

  @Composable
  fun primarySuccessColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.successPrimary,
      contentColor = AppTheme.colors.backgroundPrimary,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonPrimaryContentDisabled
    )
  }

  @Composable
  fun secondaryDefaultColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonSecondaryBg,
      contentColor = AppTheme.colors.contentPrimary,
      disabledContainerColor = AppTheme.colors.buttonSecondaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonSecondaryContentDisabled
    )
  }

  @Composable
  fun secondaryErrorColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonSecondaryBg,
      contentColor = AppTheme.colors.errorPrimary,
      disabledContainerColor = AppTheme.colors.buttonSecondaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonSecondaryContentDisabled
    )
  }

  @Composable
  fun secondarySuccessColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.buttonSecondaryBg,
      contentColor = AppTheme.colors.successPrimary,
      disabledContainerColor = AppTheme.colors.buttonSecondaryBgDisabled,
      disabledContentColor = AppTheme.colors.buttonSecondaryContentDisabled
    )
  }

  @Composable
  fun tertiaryDefaultColors(): ButtonColors {
    return ButtonDefaultsInternal.buttonColors(
      containerColor = AppTheme.colors.backgroundAccentPrimary,
      contentColor = AppTheme.colors.cardAccent,
      disabledContainerColor = AppTheme.colors.buttonPrimaryBgDisabled,
      disabledContentColor = AppTheme.colors.backgroundPrimary
    )
  }

  @Composable
  fun textDefaultColors(): ButtonColors {
    return ButtonDefaultsInternal.textButtonColors(
      contentColor = AppTheme.colors.buttonTertiaryContent,
      disabledContentColor = AppTheme.colors.buttonTertiaryContentDisabled
    )
  }

  @Composable
  fun textErrorColors(): ButtonColors {
    return ButtonDefaultsInternal.textButtonColors(
      contentColor = AppTheme.colors.errorPrimary,
      disabledContentColor = AppTheme.colors.buttonTertiaryContentDisabled
    )
  }

  @Composable
  fun textSuccessColors(): ButtonColors {
    return ButtonDefaultsInternal.textButtonColors(
      contentColor = AppTheme.colors.successPrimary,
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
  val style: ButtonStyle,
  val label: String,
  val enabled: Boolean,
  val showLoading: Boolean,
  val showElevation: Boolean
)

internal class ButtonPreviewStateProvider : PreviewParameterProvider<ButtonPreviewState> {
  override val values = sequenceOf(
    // Disabled Style
    disabled.copy(style = ButtonStyle.Default),
    disabledShowLoading.copy(style = ButtonStyle.Default),
    // Default Style
    default.copy(style = ButtonStyle.Default),
    showLoading.copy(style = ButtonStyle.Default),
    // Error Style
    default.copy(style = ButtonStyle.Error),
    showLoading.copy(style = ButtonStyle.Error),
    // Success Style
    default.copy(style = ButtonStyle.Success),
    showLoading.copy(style = ButtonStyle.Success)
  )
}

private val default = ButtonPreviewState(
  label = "default",
  style = ButtonStyle.Default,
  enabled = true,
  showLoading = false,
  showElevation = true
)

private val showLoading = ButtonPreviewState(
  label = "showLoading",
  style = ButtonStyle.Default,
  enabled = true,
  showLoading = true,
  showElevation = false
)

private val disabled = ButtonPreviewState(
  label = "disabled",
  style = ButtonStyle.Default,
  enabled = false,
  showLoading = false,
  showElevation = false
)

private val disabledShowLoading = ButtonPreviewState(
  label = "disabled",
  style = ButtonStyle.Default,
  enabled = false,
  showLoading = true,
  showElevation = false
)
