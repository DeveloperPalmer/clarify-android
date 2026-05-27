package ru.sla.clarify.feature.login.ui.screen.credentials

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.ColorTheme

@Preview
@Composable
private fun CredentialsScreenPreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    CredentialsContent(
      googleInProgress = true,
      onSignInByEmail = {},
      onSignInByGoogle = {}
    )
  }
}

@Preview
@Composable
private fun CredentialsScreenPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    CredentialsContent(
      googleInProgress = false,
      onSignInByEmail = {},
      onSignInByGoogle = {}
    )
  }
}
