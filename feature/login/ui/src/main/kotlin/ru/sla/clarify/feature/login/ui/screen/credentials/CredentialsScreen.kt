package ru.sla.clarify.feature.login.ui.screen.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.uikit.component.Divider
import ru.sla.clarify.uikit.component.button.PrimaryIconButton
import ru.sla.clarify.uikit.component.button.SecondaryButton
import ru.sla.clarify.uikit.component.icon.GoogleIcon
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun CredentialsScreen(viewModel: CredentialsViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { _, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    ScreenScaffold(state = scaffoldState) {
      CredentialsContent(
        onSignInByEmail = intents.signInByEmail,
        onSignInByGoogle = intents.signInByGoogle
      )
    }
  }
}

@Composable
internal fun CredentialsContent(
  onSignInByEmail: () -> Unit,
  onSignInByGoogle: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(AppTheme.colors.backgroundPrimary)
      .systemBarsPadding()
      .displayCutoutPadding(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    VSpacer(24.dp)
    Text(
      modifier = Modifier.padding(horizontal = 24.dp),
      text = stringResource(R.string.enter),
      color = AppTheme.colors.contentPrimary,
      style = AppTheme.typography.display3
    )
    VSpacer(12.dp)
    Text(
      modifier = Modifier.padding(horizontal = 48.dp),
      text = stringResource(R.string.credentials_enter_description),
      color = AppTheme.colors.contentSecondary,
      style = AppTheme.typography.body1,
      textAlign = TextAlign.Center
    )
    VSpacer(24.dp)
    PrimaryIconButton(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      trailingIcon = { GoogleIcon() },
      text = stringResource(R.string.google_sign_in),
      onClick = onSignInByGoogle
    )
    VSpacer(16.dp)
    OptionsSeparator(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    )
    VSpacer(16.dp)
    SecondaryButton(
      modifier = Modifier
        .heightIn(56.dp)
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      text = stringResource(R.string.credentials_enter_by_email),
      onClick = onSignInByEmail
    )
  }
}

@Composable
private fun OptionsSeparator(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Divider(modifier = Modifier.weight(1f))
    HSpacer(16.dp)
    Text(
      text = stringResource(R.string.or),
      color = AppTheme.colors.contentSecondary,
      style = AppTheme.typography.body3
    )
    HSpacer(16.dp)
    Divider(modifier = Modifier.weight(1f))
  }
}
