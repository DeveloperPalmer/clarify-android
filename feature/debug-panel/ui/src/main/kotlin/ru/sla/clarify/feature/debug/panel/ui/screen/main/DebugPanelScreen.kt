package ru.sla.clarify.feature.debug.panel.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.feature.debug.panel.domain.entity.UserJsonError
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.component.textfield.PrimaryTextField
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun DebugPanelScreen(viewModel: DebugPanelViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    BackHandler(onBack = intents.navigateBack)
    ScreenScaffold(state = scaffoldState) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = stringResource(R.string.debug_panel_title),
          style = AppTheme.typography.display3
        )
        PrimaryTextField(
          modifier = Modifier.fillMaxWidth(),
          value = state.userField,
          onValueChange = intents.changeUserField,
          isError = state.userJsonError != null
        )
        PrimaryButton(
          modifier = Modifier.fillMaxWidth(),
          onClick = intents.createUser,
          text = stringResource(R.string.debug_panel_send),
          showLoading = state.userSending,
          enabled = state.userField.isNotBlank()
        )
      }
    }
  }
}

@Composable
private fun UserJsonError.message(): String = when (this) {
  is UserJsonError.MalformedJson -> {
    stringResource(R.string.debug_panel_error_malformed_json)
  }
  is UserJsonError.MissingField -> {
    stringResource(R.string.debug_panel_error_missing_field, field)
  }
  is UserJsonError.UnknownField -> {
    stringResource(R.string.debug_panel_error_unknown_field, field)
  }
  is UserJsonError.InvalidPhotoUrl -> {
    stringResource(R.string.debug_panel_error_invalid_photo_url)
  }
  is UserJsonError.UserAlreadyExist -> {
    stringResource(R.string.debug_panel_error_invalid_email)
  }
}
