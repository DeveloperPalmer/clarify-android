package ru.sla.clarify.feature.main.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MainScreen(viewModel: MainViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    scaffoldState.dialogError = state.dialogError
    scaffoldState.snackbarError = state.snackbarError
    ScreenScaffold(
      state = scaffoldState,
      onDismissDialogError = intents.dismissDialogError,
      onDismissSnackbarError = intents.dismissSnackbarError
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          modifier = Modifier.align(Alignment.End),
          onClick = intents.logout
        ) {
          Text("logout")
        }
      }
    }
  }
}
