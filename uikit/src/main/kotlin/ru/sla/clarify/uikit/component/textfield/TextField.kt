package ru.sla.clarify.uikit.component.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef
import androidx.compose.material3.TextFieldDefaults as TextFieldDefaultsInternal

@Composable
internal fun TextFieldInternal(
  value: String,
  onValueChange: (String) -> Unit,
  colors: TextFieldColors,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  enabled: Boolean = true,
  isError: Boolean = false,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  maxVisibleLines: Int = DEFAULT_MAX_VISIBLE_LINES,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
  val focused = interactionSource.collectIsFocusedAsState().value
  val textColor = colors.textColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val containerColor = colors.containerColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val placeholderColor = colors.placeholderColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val cursorColor = colors.cursorColor(
    isError = isError
  )
  BasicTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    minLines = 1,
    maxLines = maxVisibleLines,
    textStyle = AppTheme.typography.body1.copy(
      color = textColor.value
    ),
    cursorBrush = SolidColor(
      value = cursorColor.value
    ),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    interactionSource = interactionSource,
    decorationBox = { innerTextField ->
      TextFieldDecoration(
        value = value,
        shape = shape,
        containerColor = containerColor.value,
        placeholderColor = placeholderColor.value,
        placeholder = placeholder,
        innerTextField = innerTextField
      )
    }
  )
}

@Composable
internal fun TextFieldInternal(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  colors: TextFieldColors,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  enabled: Boolean = true,
  isError: Boolean = false,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  maxVisibleLines: Int = DEFAULT_MAX_VISIBLE_LINES,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
  val focused = interactionSource.collectIsFocusedAsState().value
  val textColor = colors.textColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val containerColor = colors.containerColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val placeholderColor = colors.placeholderColor(
    enabled = enabled,
    isError = isError,
    focused = focused
  )
  val cursorColor = colors.cursorColor(
    isError = isError
  )
  BasicTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    minLines = 1,
    maxLines = maxVisibleLines,
    textStyle = AppTheme.typography.body1.copy(
      color = textColor.value
    ),
    cursorBrush = SolidColor(
      value = cursorColor.value
    ),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    interactionSource = interactionSource,
    decorationBox = { innerTextField ->
      TextFieldDecoration(
        value = value.text,
        shape = shape,
        containerColor = containerColor.value,
        placeholderColor = placeholderColor.value,
        placeholder = placeholder,
        innerTextField = innerTextField
      )
    }
  )
}

@Composable
private fun TextFieldDecoration(
  value: String,
  shape: Shape,
  containerColor: Color,
  placeholderColor: Color,
  placeholder: TextRef,
  modifier: Modifier = Modifier,
  innerTextField: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .surface(
        shape = shape,
        backgroundColor = containerColor
      )
      .heightIn(
        min = SendButtonSize
      )
      .padding(
        vertical = 12.dp,
        horizontal = 16.dp
      ),
    contentAlignment = Alignment.CenterStart
  ) {
    if (value.isEmpty()) {
      Text(
        text = resolveTextRef(placeholder),
        style = AppTheme.typography.body1,
        color = placeholderColor
      )
    }
    innerTextField()
  }
}

internal object TextFieldDefaults {
  @Composable
  fun primaryDefaultColors(): TextFieldColors {
    return TextFieldDefaultsInternal.colors(
      focusedTextColor = AppTheme.colors.contentPrimary,
      unfocusedTextColor = AppTheme.colors.contentPrimary,
      disabledTextColor = AppTheme.colors.contentTertiary,
      errorTextColor = AppTheme.colors.contentPrimary,
      focusedContainerColor = AppTheme.colors.cardSecondary,
      unfocusedContainerColor = AppTheme.colors.cardSecondary,
      disabledContainerColor = AppTheme.colors.cardSecondary,
      errorContainerColor = AppTheme.colors.cardSecondary,
      cursorColor = AppTheme.colors.contentAccentPrimary,
      errorCursorColor = AppTheme.colors.errorPrimary,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      disabledIndicatorColor = Color.Transparent,
      errorIndicatorColor = Color.Transparent,
      focusedPlaceholderColor = AppTheme.colors.contentSecondary,
      unfocusedPlaceholderColor = AppTheme.colors.contentSecondary,
      disabledPlaceholderColor = AppTheme.colors.contentTertiary,
      errorPlaceholderColor = AppTheme.colors.errorPrimary
    )
  }

  @Composable
  fun secondaryDefaultColors(): TextFieldColors {
    return TextFieldDefaultsInternal.colors(
      focusedTextColor = AppTheme.colors.contentPrimary,
      unfocusedTextColor = AppTheme.colors.contentPrimary,
      disabledTextColor = AppTheme.colors.contentTertiary,
      errorTextColor = AppTheme.colors.contentPrimary,
      focusedContainerColor = AppTheme.colors.cardTertiary,
      unfocusedContainerColor = AppTheme.colors.cardTertiary,
      disabledContainerColor = AppTheme.colors.cardTertiary,
      errorContainerColor = AppTheme.colors.cardTertiary,
      cursorColor = AppTheme.colors.contentAccentPrimary,
      errorCursorColor = AppTheme.colors.errorPrimary,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      disabledIndicatorColor = Color.Transparent,
      errorIndicatorColor = Color.Transparent,
      focusedPlaceholderColor = AppTheme.colors.contentSecondary,
      unfocusedPlaceholderColor = AppTheme.colors.contentSecondary,
      disabledPlaceholderColor = AppTheme.colors.contentTertiary,
      errorPlaceholderColor = AppTheme.colors.errorPrimary
    )
  }
}

@Composable
private fun TextFieldColors.textColor(
  enabled: Boolean,
  isError: Boolean,
  focused: Boolean
): State<Color> {
  return rememberUpdatedState(
    when {
      !enabled -> disabledTextColor
      isError -> errorTextColor
      focused -> focusedTextColor
      else -> unfocusedTextColor
    }
  )
}

@Composable
private fun TextFieldColors.containerColor(
  enabled: Boolean,
  isError: Boolean,
  focused: Boolean
): State<Color> {
  return rememberUpdatedState(
    when {
      !enabled -> disabledContainerColor
      isError -> errorContainerColor
      focused -> focusedContainerColor
      else -> unfocusedContainerColor
    }
  )
}

@Composable
private fun TextFieldColors.placeholderColor(
  enabled: Boolean,
  isError: Boolean,
  focused: Boolean
): State<Color> {
  return rememberUpdatedState(
    when {
      !enabled -> disabledPlaceholderColor
      isError -> errorPlaceholderColor
      focused -> focusedPlaceholderColor
      else -> unfocusedPlaceholderColor
    }
  )
}

@Composable
private fun TextFieldColors.cursorColor(isError: Boolean): State<Color> {
  return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
}

internal val SendButtonSize = 44.dp
internal const val DEFAULT_MAX_VISIBLE_LINES = 7

internal data class TextFieldPreviewState(
  val label: String,
  val text: String,
  val enabled: Boolean,
  val isError: Boolean
)

internal class TextFieldPreviewStateProvider : PreviewParameterProvider<TextFieldPreviewState> {
  override val values = sequenceOf(
    TextFieldPreviewState(
      label = "empty",
      text = "",
      enabled = true,
      isError = false
    ),
    TextFieldPreviewState(
      label = "single line",
      text = "Привет!",
      enabled = true,
      isError = false
    ),
    TextFieldPreviewState(
      label = "multiline",
      text = PREVIEW_MULTILINE_TEXT,
      enabled = true,
      isError = false
    ),
    TextFieldPreviewState(
      label = "overflow",
      text = PREVIEW_OVERFLOW_TEXT,
      enabled = true,
      isError = false
    ),
    TextFieldPreviewState(
      label = "scroll",
      text = PREVIEW_SCROLL_TEXT,
      enabled = true,
      isError = false
    ),
    TextFieldPreviewState(
      label = "error",
      text = "",
      enabled = true,
      isError = true
    ),
    TextFieldPreviewState(
      label = "disabled",
      text = "Сообщение нельзя отправить",
      enabled = false,
      isError = false
    )
  )
}

private const val PREVIEW_MULTILINE_TEXT = "Первая строка\nВторая строка\nТретья строка"
private const val PREVIEW_OVERFLOW_TEXT =
  "Сейчас пришлю последнюю версию. Здесь намеренно много текста, чтобы поле выросло до семи строк, " +
    "а затем перестало расти и начало скроллиться по вертикали, как в Telegram, " +
    "оставляя каретку в зоне видимости при наборе новых строк сообщения. " +
    "Дальше текст продолжается, чтобы строк точно стало больше семи."
private const val PREVIEW_SCROLL_TEXT =
  "Строка 1\nСтрока 2\nСтрока 3\nСтрока 4\nСтрока 5\nСтрока 6\n" +
    "Строка 7\nСтрока 8\nСтрока 9\nСтрока 10\nСтрока 11\nСтрока 12"
