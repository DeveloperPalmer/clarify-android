package ru.sla.clarify.uikit.component.textfield

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef

@Composable
fun ChatTextField(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  sendEnabled: Boolean = true,
  sendIconRes: Int = R.drawable.ic_send_24,
  header: (@Composable () -> Unit)? = null,
  contentFadeKey: Any? = null,
  focusRequestKey: Any? = null,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Шапка держит слот, пока играет exit-анимация: последний непустой контент лачится и
    // отпускается вместе с завершением AnimatedVisibility (паттерн displayedMessage в ChatCommits).
    var displayedHeader by remember { mutableStateOf(header) }
    if (header != null) {
      displayedHeader = header
    }
    // Ширина слота максимальна сразу (fillMaxWidth фиксирует её до animateContentSize), поэтому
    // анимируется только высота. Клип со скруглением стоит СНАРУЖИ animateContentSize (как surface
    // у поля ввода): контейнер рисуется по анимированному размеру, поэтому его нижняя кромка
    // приколота к полю (BottomStart), а сам он на глазах вырастает снизу вверх с целыми скруглениями,
    // проявляя контент уже внутри себя — вместо того чтобы возникнуть сразу полной высотой.
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(AppTheme.shapes.round12)
        .animateContentSize(
          animationSpec = AppTheme.motion.mediumTween(),
          alignment = Alignment.BottomStart
        )
    ) {
      // Полное имя: без него резолвится ColumnScope.AnimatedVisibility внешней Column.
      androidx.compose.animation.AnimatedVisibility(
        visible = header != null,
        enter = fadeIn(AppTheme.motion.mediumTween()),
        exit = fadeOut(AppTheme.motion.mediumTween())
      ) {
        Box(modifier = Modifier.padding(bottom = 8.dp)) {
          displayedHeader?.invoke()
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      PrimaryTextField(
        modifier = Modifier.weight(1f),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        contentFadeKey = contentFadeKey,
        focusRequestKey = focusRequestKey
      )
      SendButton(
        enabled = enabled && sendEnabled && value.isNotBlank(),
        iconRes = sendIconRes,
        onClick = {
          onSend()
          onClear()
        }
      )
    }
  }
}

@Composable
private fun SendButton(
  enabled: Boolean,
  iconRes: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val backgroundColor = animateColorAsState(
    label = "backgroundColor",
    targetValue = if (enabled) {
      AppTheme.colors.buttonPrimaryBg
    } else {
      AppTheme.colors.buttonPrimaryBgDisabled
    }
  )
  Box(
    modifier = modifier
      .size(SendButtonSize)
      .focusProperties { canFocus = false }
      .surface(
        backgroundColor = { backgroundColor.value },
        shape = CircleShape,
        enabled = enabled,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    val iconTint = animateColorAsState(
      label = "iconTint",
      targetValue = if (enabled) {
        AppTheme.colors.buttonPrimaryContent
      } else {
        AppTheme.colors.buttonPrimaryContentDisabled
      }
    )
    AnimatedContent(
      targetState = iconRes,
      transitionSpec = AppTheme.motion.mediumTransitionSpec(),
      label = "sendIcon"
    ) { targetIconRes ->
      Icon(
        painter = painterResource(targetIconRes),
        contentDescription = stringResource(R.string.chat_input_send_button),
        tint = { iconTint.value }
      )
    }
  }
}

object ChatTextFieldDefaults {
  @Composable
  fun EditHeader(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
  ) {
    EditHeader(
      modifier = modifier,
      iconRes = R.drawable.ic_pencil_24,
      title = resRef(R.string.thread_edit_header_title),
      text = text,
      onClose = onClose
    )
  }
}

@Composable
private fun EditHeader(
  iconRes: Int,
  title: TextRef,
  text: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .height(IntrinsicSize.Max)
      .fillMaxWidth()
      .surface(
        shape = AppTheme.shapes.round12,
        backgroundColor = AppTheme.colors.cardSecondary
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HSpacer(12.dp)
    Icon(
      modifier = Modifier.size(24.dp),
      painter = painterResource(iconRes),
      tint = AppTheme.colors.contentAccentPrimary,
      contentDescription = null
    )
    HSpacer(8.dp)
    Box(
      modifier = Modifier
        .width(3.dp)
        .fillMaxHeight(0.7f)
        .clip(CircleShape)
        .background(AppTheme.colors.contentAccentPrimary)
    )
    HSpacer(8.dp)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = resolveTextRef(title),
        style = AppTheme.typography.label3Bold,
        color = AppTheme.colors.contentAccentPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      AnimatedContent(
        targetState = text,
        transitionSpec = AppTheme.motion.mediumTransitionSpec(),
        label = "composerHeaderSnippet"
      ) { snippetText ->
        Text(
          modifier = Modifier.fillMaxWidth(),
          text = snippetText,
          style = AppTheme.typography.body3,
          color = AppTheme.colors.contentSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    IconAction(
      iconResId = R.drawable.ic_close_24,
      iconTint = AppTheme.colors.contentPrimary,
      onClick = onClose
    )
  }
}

@Preview
@Composable
private fun ChatTextFieldPreviewLight(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    ChatTextFieldPreviewContent(state)
  }
}

@Preview
@Composable
private fun ChatTextFieldPreviewDark(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    ChatTextFieldPreviewContent(state)
  }
}

@Composable
private fun ChatTextFieldPreviewContent(state: TextFieldPreviewState) {
  Column(
    modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ChatTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.text,
      onValueChange = {},
      onSend = {},
      onClear = {},
      enabled = state.enabled
    )
  }
}

@Preview
@Composable
private fun ComposerHeaderPreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    ComposerHeaderPreviewContent()
  }
}

@Preview
@Composable
private fun ComposerHeaderPreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    ComposerHeaderPreviewContent()
  }
}

@Composable
private fun ComposerHeaderPreviewContent() {
  Box(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(8.dp)
  ) {
    EditHeader(
      iconRes = R.drawable.ic_pencil_24,
      title = strRef("Редактировать сообщение"),
      text = "Поправил отступ снизу — кнопка больше не наезжает на картинку",
      onClose = {}
    )
  }
}
