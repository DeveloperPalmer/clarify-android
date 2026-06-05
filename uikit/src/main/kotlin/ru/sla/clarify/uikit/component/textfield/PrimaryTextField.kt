package ru.sla.clarify.uikit.component.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef

@Composable
fun PrimaryTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isError: Boolean = false,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  TextFieldInternal(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    shape = RoundedCornerShape(16.dp),
    enabled = enabled,
    isError = isError,
    placeholder = placeholder,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions
  )
}

@Preview
@Composable
private fun PrimaryTextFieldPreviewLight(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    PrimaryTextFieldPreviewContent(state)
  }
}

@Preview
@Composable
private fun PrimaryTextFieldPreviewDark(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    PrimaryTextFieldPreviewContent(state)
  }
}

@Composable
private fun PrimaryTextFieldPreviewContent(state: TextFieldPreviewState) {
  Column(
    modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    PrimaryTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.text,
      onValueChange = {},
      enabled = state.enabled,
      isError = state.isError
    )
  }
}
