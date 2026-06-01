package ru.sla.clarify.uikit.component.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.AppTheme.colors
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef

@Composable
fun ChatTextField(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  maxVisibleLines: Int = DEFAULT_MAX_VISIBLE_LINES
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
      enabled = enabled,
      minLines = 1,
      maxLines = maxVisibleLines,
      textStyle = AppTheme.typography.body1.copy(
        color = colors.contentPrimary
      ),
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences
      ),
      cursorBrush = SolidColor(
        value = colors.contentAccentPrimary
      ),
      decorationBox = { innerTextField ->
        TextFieldDecoration(
          value = value,
          placeholder = placeholder,
          innerTextField = innerTextField
        )
      }
    )
    SendButton(
      enabled = enabled && value.isNotBlank(),
      onClick = {
        val text = value.trim()
        if (text.isNotEmpty()) {
          onSend()
          onClear()
        }
      }
    )
  }
}

@Composable
private fun TextFieldDecoration(
  value: String,
  placeholder: TextRef,
  modifier: Modifier = Modifier,
  innerTextField: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .surface(
        shape = RoundedCornerShape(28.dp),
        backgroundColor = colors.cardSecondary
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
        color = colors.contentSecondary
      )
    }
    innerTextField()
  }
}

@Composable
private fun SendButton(
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val backgroundColor by animateColorAsState(
    label = "backgroundColor",
    targetValue = if (enabled) colors.buttonPrimaryBg else colors.buttonPrimaryBgDisabled
  )
  val iconTint by animateColorAsState(
    label = "iconTint",
    targetValue = if (enabled) colors.buttonPrimaryContent else colors.buttonPrimaryContentDisabled
  )
  Box(
    modifier = modifier
      .size(SendButtonSize)
      .focusProperties { canFocus = false }
      .surface(
        backgroundColor = backgroundColor,
        shape = CircleShape,
        enabled = enabled,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_send_24),
      tint = iconTint,
      contentDescription = stringResource(R.string.chat_input_send_button)
    )
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChatTextFieldPreviewLight(
  @PreviewParameter(ChatTextFieldPreviewStateProvider::class)
  state: ChatTextFieldPreviewState
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    ChatTextFieldPreviewContent(state)
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChatTextFieldPreviewDark(
  @PreviewParameter(ChatTextFieldPreviewStateProvider::class)
  state: ChatTextFieldPreviewState
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    ChatTextFieldPreviewContent(state)
  }
}

@Composable
private fun ChatTextFieldPreviewContent(state: ChatTextFieldPreviewState) {
  var value by remember { mutableStateOf(state.text) }
  ChatTextField(
    value = value,
    onValueChange = { value = it },
    onSend = {},
    onClear = {},
    enabled = state.enabled
  )
}

private data class ChatTextFieldPreviewState(
  val label: String,
  val text: String,
  val enabled: Boolean
)

private class ChatTextFieldPreviewStateProvider :
  PreviewParameterProvider<ChatTextFieldPreviewState> {
  override val values = sequenceOf(
    ChatTextFieldPreviewState(
      label = "empty",
      text = "",
      enabled = true
    ),
    ChatTextFieldPreviewState(
      label = "single line",
      text = "Привет!",
      enabled = true
    ),
    ChatTextFieldPreviewState(
      label = "multiline",
      text = PREVIEW_MULTILINE_TEXT,
      enabled = true
    ),
    ChatTextFieldPreviewState(
      label = "overflow",
      text = PREVIEW_OVERFLOW_TEXT,
      enabled = true
    ),
    ChatTextFieldPreviewState(
      label = "scroll",
      text = PREVIEW_SCROLL_TEXT,
      enabled = true
    ),
    ChatTextFieldPreviewState(
      label = "disabled",
      text = "Сообщение нельзя отправить",
      enabled = false
    )
  )
}

private const val DEFAULT_MAX_VISIBLE_LINES = 7
private val SendButtonSize = 44.dp

private const val PREVIEW_MULTILINE_TEXT = "Первая строка\nВторая строка\nТретья строка"
private const val PREVIEW_OVERFLOW_TEXT =
  "Сейчас пришлю последнюю версию. Здесь намеренно много текста, чтобы поле выросло до семи строк, " +
    "а затем перестало расти и начало скроллиться по вертикали, как в Telegram, " +
    "оставляя каретку в зоне видимости при наборе новых строк сообщения. " +
    "Дальше текст продолжается, чтобы строк точно стало больше семи."
private const val PREVIEW_SCROLL_TEXT =
  "Строка 1\nСтрока 2\nСтрока 3\nСтрока 4\nСтрока 5\nСтрока 6\n" +
    "Строка 7\nСтрока 8\nСтрока 9\nСтрока 10\nСтрока 11\nСтрока 12"
