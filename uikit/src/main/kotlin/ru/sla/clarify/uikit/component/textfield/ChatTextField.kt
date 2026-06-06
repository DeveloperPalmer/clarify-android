package ru.sla.clarify.uikit.component.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.TextRef
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
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  Row(
    modifier = modifier.fillMaxWidth(),
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
      keyboardActions = keyboardActions
    )
    SendButton(
      enabled = enabled && value.isNotBlank(),
      onClick = {
        onSend()
        onClear()
      }
    )
  }
}

@Composable
private fun SendButton(
  enabled: Boolean,
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
    Icon(
      painter = painterResource(R.drawable.ic_send_24),
      contentDescription = stringResource(R.string.chat_input_send_button),
      tint = { iconTint.value }
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
