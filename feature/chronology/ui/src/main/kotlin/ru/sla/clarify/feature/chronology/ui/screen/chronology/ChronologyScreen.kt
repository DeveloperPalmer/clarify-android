package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChronologyScreen(viewModel: ChronologyViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, _ ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    ScreenScaffold(state = scaffoldState) {
      Box(modifier = Modifier.fillMaxSize()) {
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          contentSize = CANVAS_SIZE,
          lanes = LANES
        ) {
          // Узлы появятся здесь: пока полотно проверяется фоном и магистралью.
        }
        Text(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 48.dp),
          text = stringResource(R.string.chronology_placeholder),
          style = AppTheme.typography.title1Bold,
          color = AppTheme.colors.contentPrimary
        )
      }
    }
  }
}

/**
 * Временные размеры полотна: пока данных нет, оно делается заведомо шире экрана —
 * иначе панорамирование не проверить.
 */
private val CANVAS_SIZE = DpSize(width = 1600.dp, height = 620.dp)
private val LANES = -2..2
