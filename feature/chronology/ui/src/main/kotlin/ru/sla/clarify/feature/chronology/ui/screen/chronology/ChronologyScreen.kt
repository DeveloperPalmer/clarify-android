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
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.MessageChipState
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap
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
      val demo = remember { demoNodes() }
      // Список узлов и таблица содержимого строятся один раз: `map` на каждой рекомпозиции давал
      // бы новый список, а поиск линейным сканом на узел — квадратичный обход графа.
      val nodes = remember(demo) { demo.map { it.node } }
      val contentById = remember(demo) { demo.associateBy { it.node.id } }
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

/**
 * Временный набор узлов вместо данных.
 *
 * Задаётся дорожками и паузами, а не координатами: положение считает геометрия полотна, и это
 * заодно проверяет, что ось X действительно выводится из паузы. Уезжает, как только появится сборка
 * графа из веток и коммитов.
 */
private fun demoNodes(): List<DemoNode> {
  return listOf(
    DemoNode(
      node = GraphNode(id = GraphNode.Id("1"), lane = 0, gap = TimeGap.Hours),
      text = "Не бьётся по срокам",
      isMine = false
    ),
    DemoNode(
      node = GraphNode(id = GraphNode.Id("2"), lane = 0, gap = TimeGap.Minutes),
      text = "Где именно?",
      isMine = true
    ),
    DemoNode(
      node = GraphNode(id = GraphNode.Id("3"), lane = 0, gap = TimeGap.Hour),
      text = "Выношу в ветку",
      isMine = true,
      chipState = MessageChipState.Edited
    ),
    DemoNode(
      node = GraphNode(id = GraphNode.Id("4"), lane = -1, gap = TimeGap.Minutes),
      text = "Готово, ветка тут",
      isMine = false,
      chipState = MessageChipState.Quoted
    ),
    DemoNode(
      node = GraphNode(id = GraphNode.Id("5"), lane = -1, gap = TimeGap.Day),
      text = "Фиксируем 14-е",
      isMine = true,
      chipState = MessageChipState.Sending
    )
  )
}
