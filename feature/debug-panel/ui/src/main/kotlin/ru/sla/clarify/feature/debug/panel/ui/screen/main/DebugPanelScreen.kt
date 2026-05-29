package ru.sla.clarify.feature.debug.panel.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState

@Composable
fun DebugPanelScreen(viewModel: DebugPanelViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    BackHandler(
      onBack = intents.navigateBack
    )
    ScreenScaffold(state = scaffoldState) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(stringResource(R.string.debug_panel_title))
        Button(onClick = intents.navigateBack) {
          Text(stringResource(R.string.debug_panel_close))
        }
      }
    }
  }
}
