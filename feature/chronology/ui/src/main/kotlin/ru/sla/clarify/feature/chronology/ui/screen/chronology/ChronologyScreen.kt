package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberGraphCanvasState
import ru.sla.clarify.feature.chronology.ui.components.node.EpisodeNode
import ru.sla.clarify.uikit.component.icon.IconAction
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
          debugOverlayVisible = state.debugOverlayAvailable && state.debugOverlayVisible
        ) { id ->
          val item = contentById.getValue(id)
          EpisodeNode(
            time = item.time,
            count = item.count,
            snippet = item.snippet,
            myShare = item.myShare,
            unreadCount = item.unreadCount,
            dim = item.dim
          )
        }
        // Обычный контейнер поверх полотна, а не тулбар: у настоящего тулбара есть фон, который
        // закрыл бы верх графа, и своя поверхность, которая съедала бы жесты — а полотно тянут
        // пальцем по всему экрану, в том числе под заголовком. Высоту задаёт кнопка, и по её центру
        // выравнивается всё остальное.
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
        ) {
          TopAppBarDefaults.NavigationIcon(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = intents.navigateBack
          )
          Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(R.string.chronology_placeholder),
            style = AppTheme.typography.title1Bold,
            color = AppTheme.colors.contentPrimary
          )
          // Кнопки нет, пока панель не разрешена тоглом: без него она ничего не переключает.
          if (state.debugOverlayAvailable) {
            IconAction(
              modifier = Modifier.align(Alignment.CenterEnd),
              iconResId = R.drawable.ic_debug_24,
              onClick = intents.toggleDebugOverlay,
              iconTint = if (state.debugOverlayVisible) {
                AppTheme.colors.contentPrimary
              } else {
                AppTheme.colors.contentTertiary
              }
            )
          }
        }
      }
    }
  }
}
