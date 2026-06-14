package ru.sla.clarify.uikit.component.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef

@Composable
fun SecondaryTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  errorText: TextRef? = null,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  Column(modifier = modifier) {
    TextFieldInternal(
      modifier = Modifier.fillMaxWidth(),
      value = value,
      onValueChange = onValueChange,
      shape = RoundedCornerShape(16.dp),
      enabled = enabled,
      isError = errorText != null,
      placeholder = placeholder,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      colors = TextFieldDefaults.secondaryDefaultColors()
    )
    if (errorText != null) {
      Text(
        modifier = Modifier.padding(top = 6.dp, start = 12.dp),
        text = resolveTextRef(errorText),
        style = AppTheme.typography.body3,
        color = AppTheme.colors.errorPrimary
      )
    }
  }
}

@Composable
fun SecondaryTextField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  errorText: TextRef? = null,
  placeholder: TextRef = resRef(R.string.chat_input_placeholder),
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  Column(modifier = modifier) {
    TextFieldInternal(
      modifier = Modifier.fillMaxWidth(),
      value = value,
      onValueChange = onValueChange,
      shape = RoundedCornerShape(16.dp),
      enabled = enabled,
      isError = errorText != null,
      placeholder = placeholder,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      colors = TextFieldDefaults.secondaryDefaultColors()
    )
    if (errorText != null) {
      Text(
        modifier = Modifier.padding(top = 6.dp, start = 12.dp),
        text = resolveTextRef(errorText),
        style = AppTheme.typography.body3,
        color = AppTheme.colors.errorPrimary
      )
    }
  }
}

@Preview
@Composable
private fun SecondaryTextFieldPreviewLight(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    SecondaryTextFieldPreviewContent(state)
  }
}

@Preview
@Composable
private fun SecondaryTextFieldPreviewDark(
  @PreviewParameter(TextFieldPreviewStateProvider::class)
  state: TextFieldPreviewState
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    SecondaryTextFieldPreviewContent(state)
  }
}

@Composable
private fun SecondaryTextFieldPreviewContent(state: TextFieldPreviewState) {
  Column(
    modifier = Modifier.background(AppTheme.colors.cardSecondary),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    SecondaryTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.text,
      onValueChange = {},
      enabled = state.enabled,
      errorText = if (state.isError) strRef("Ошибка валидации") else null
    )
  }
}
