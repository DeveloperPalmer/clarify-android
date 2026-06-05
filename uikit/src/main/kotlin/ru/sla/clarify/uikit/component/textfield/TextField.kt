package ru.sla.clarify.uikit.component.textfield

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef

@Composable
internal fun TextFieldInternal(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  enabled: Boolean = true,
  isError: Boolean = false,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  maxVisibleLines: Int = DEFAULT_MAX_VISIBLE_LINES,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  BasicTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    minLines = 1,
    maxLines = maxVisibleLines,
    textStyle = AppTheme.typography.body1.copy(
      color = AppTheme.colors.contentPrimary
    ),
    cursorBrush = SolidColor(
      value = AppTheme.colors.contentAccentPrimary
    ),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    decorationBox = { innerTextField ->
      TextFieldDecoration(
        value = value,
        shape = shape,
        isError = isError,
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
  isError: Boolean,
  placeholder: TextRef,
  modifier: Modifier = Modifier,
  innerTextField: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .surface(
        shape = shape,
        backgroundColor = AppTheme.colors.cardSecondary
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
        color = if (isError) AppTheme.colors.errorPrimary else AppTheme.colors.contentSecondary
      )
    }
    innerTextField()
  }
}

internal val SendButtonSize = 44.dp
internal const val DEFAULT_MAX_VISIBLE_LINES = 7

internal data class TextFieldPreviewState(
  val label: String,
  val text: String,
  val enabled: Boolean,
  val isError: Boolean
)

internal class TextFieldPreviewStateProvider :
  PreviewParameterProvider<TextFieldPreviewState> {
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
