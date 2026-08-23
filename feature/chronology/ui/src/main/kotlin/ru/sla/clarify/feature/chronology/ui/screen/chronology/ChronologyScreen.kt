package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphDebugOverlay
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
      val canvasState = rememberGraphCanvasState(nodes = state.nodes)
      Box(modifier = Modifier.fillMaxSize()) {
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          state = canvasState,
          node = { id ->
            val item = state.episodeById.getValue(id)
            EpisodeNode(
              time = item.time,
              count = item.count,
              snippet = item.snippet,
              myShare = item.myShare,
              unreadCount = item.unreadCount,
              dim = item.dim
            )
          },
          overlay = { onBoundsChanged ->
            if (state.debugOverlayAvailable && state.debugOverlayVisible) {
              GraphDebugOverlay(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .navigationBarsPadding()
                  .fillMaxWidth()
                  .padding(12.dp),
                state = canvasState,
                onBoundsChanged = onBoundsChanged
              )
            }
          }
        )
        ChronologyToolbar(
          modifier = Modifier.padding(horizontal = 4.dp),
          debugOverlayVisible = state.debugOverlayVisible,
          debugOverlayAvailable = state.debugOverlayAvailable,
          onBack = intents.navigateBack,
          onToggleDebugOverlay = intents.toggleDebugOverlay
        )
      }
    }
  }
}

@Composable
private fun ChronologyToolbar(
  debugOverlayVisible: Boolean,
  debugOverlayAvailable: Boolean,
  onBack: () -> Unit,
  onToggleDebugOverlay: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    TopAppBarDefaults.NavigationIcon(
      modifier = Modifier.align(Alignment.CenterStart),
      onClick = onBack
    )
    Text(
      modifier = Modifier.align(Alignment.Center),
      text = stringResource(R.string.chronology_placeholder),
      style = AppTheme.typography.title1Bold,
      color = AppTheme.colors.contentPrimary
    )
    if (debugOverlayAvailable) {
      IconAction(
        modifier = Modifier.align(Alignment.CenterEnd),
        iconResId = R.drawable.ic_debug_24,
        iconTint = if (debugOverlayVisible) {
          AppTheme.colors.contentPrimary
        } else {
          AppTheme.colors.contentTertiary
        },
        onClick = onToggleDebugOverlay
      )
    }
  }
}
