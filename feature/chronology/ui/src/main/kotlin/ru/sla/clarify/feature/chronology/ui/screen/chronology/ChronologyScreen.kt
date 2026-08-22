package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.MessageChip
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberGraphCanvasState
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChronologyScreen(viewModel: ChronologyViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    ScreenScaffold(state = scaffoldState) {
      val mocks = remember { mockNodes() }
      // Список узлов и таблица содержимого строятся один раз: `map` на каждой рекомпозиции давал
      // бы новый список, а поиск линейным сканом на узел — квадратичный обход графа.
      val nodes = remember(mocks) { mocks.map { it.node } }
      val contentById = remember(mocks) { mocks.associateBy { it.node.id } }
      val canvasState = rememberGraphCanvasState(nodes = nodes)
      Box(modifier = Modifier.fillMaxSize()) {
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          state = canvasState,
          debugOverlayVisible = state.debugOverlayVisible
        ) { id ->
          val item = contentById.getValue(id)
          MessageChip(
            text = item.text,
            isMine = item.isMine,
            state = item.chipState
          )
        }
        TopAppBarDefaults.NavigationIcon(
          modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding(),
          onClick = intents.navigateBack
        )
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
