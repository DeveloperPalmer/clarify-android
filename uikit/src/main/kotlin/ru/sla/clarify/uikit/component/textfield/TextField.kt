package ru.sla.clarify.uikit.component.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import ru.sla.clarify.uikit.theme.AppTheme
import androidx.compose.material3.OutlinedTextField as MaterialOutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults as MaterialOutlinedTextFieldDefaults
import androidx.compose.material3.TextField as MaterialTextField
import androidx.compose.material3.TextFieldDefaults as MaterialTextFieldDefaults

@Composable
fun TextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  interactionSource: MutableInteractionSource? = null,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null
) {
  MaterialTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    readOnly = readOnly,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    minLines = minLines,
    maxLines = maxLines,
    interactionSource = interactionSource,
    colors = TextFieldDefaults.textFieldColors()
  )
}

@Composable
fun OutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  isError: Boolean = false,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  interactionSource: MutableInteractionSource? = null,
  label: @Composable (() -> Unit)? = null,
  placeholder: @Composable (() -> Unit)? = null,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  prefix: @Composable (() -> Unit)? = null,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null
) {
  MaterialOutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    enabled = enabled,
    readOnly = readOnly,
    label = label,
    placeholder = placeholder,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    prefix = prefix,
    suffix = suffix,
    supportingText = supportingText,
    isError = isError,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    minLines = minLines,
    maxLines = maxLines,
    interactionSource = interactionSource,
    colors = TextFieldDefaults.outlinedTextFieldColors()
  )
}

internal object TextFieldDefaults {
  @Composable
  fun textFieldColors(): TextFieldColors {
    return MaterialTextFieldDefaults.colors(
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
      focusedIndicatorColor = AppTheme.colors.contentAccentPrimary,
      unfocusedIndicatorColor = AppTheme.colors.contentTertiary,
      disabledIndicatorColor = AppTheme.colors.contentQuaternary,
      errorIndicatorColor = AppTheme.colors.errorPrimary,
      focusedPlaceholderColor = AppTheme.colors.contentSecondary,
      unfocusedPlaceholderColor = AppTheme.colors.contentSecondary,
      disabledPlaceholderColor = AppTheme.colors.contentTertiary,
      errorPlaceholderColor = AppTheme.colors.contentSecondary,
      focusedLabelColor = AppTheme.colors.contentAccentPrimary,
      unfocusedLabelColor = AppTheme.colors.contentSecondary,
      disabledLabelColor = AppTheme.colors.contentTertiary,
      errorLabelColor = AppTheme.colors.errorPrimary
    )
  }

  @Composable
  fun outlinedTextFieldColors(): TextFieldColors {
    return MaterialOutlinedTextFieldDefaults.colors(
      focusedTextColor = AppTheme.colors.contentPrimary,
      unfocusedTextColor = AppTheme.colors.contentPrimary,
      disabledTextColor = AppTheme.colors.contentTertiary,
      errorTextColor = AppTheme.colors.contentPrimary,
      focusedContainerColor = Color.Transparent,
      unfocusedContainerColor = Color.Transparent,
      disabledContainerColor = Color.Transparent,
      errorContainerColor = Color.Transparent,
      cursorColor = AppTheme.colors.contentAccentPrimary,
      errorCursorColor = AppTheme.colors.errorPrimary,
      focusedBorderColor = AppTheme.colors.contentAccentPrimary,
      unfocusedBorderColor = AppTheme.colors.contentTertiary,
      disabledBorderColor = AppTheme.colors.contentQuaternary,
      errorBorderColor = AppTheme.colors.errorPrimary,
      focusedPlaceholderColor = AppTheme.colors.contentSecondary,
      unfocusedPlaceholderColor = AppTheme.colors.contentSecondary,
      disabledPlaceholderColor = AppTheme.colors.contentTertiary,
      errorPlaceholderColor = AppTheme.colors.contentSecondary,
      focusedLabelColor = AppTheme.colors.contentAccentPrimary,
      unfocusedLabelColor = AppTheme.colors.contentSecondary,
      disabledLabelColor = AppTheme.colors.contentTertiary,
      errorLabelColor = AppTheme.colors.errorPrimary
    )
  }
}
