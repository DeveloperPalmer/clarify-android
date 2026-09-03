package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import ru.sla.atlas.debug.DebugOverlay
import ru.sla.atlas.ui.rememberAtlasCanvasState
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.CameraButtons
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphMinimap
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberMergeCeremonyState
import ru.sla.clarify.feature.chronology.ui.entity.ChronologyLevels
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.mapper.GraphNode
import ru.sla.clarify.feature.chronology.ui.mapper.toDebugLabel
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.component.scrim.ScrimEffect
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
    BackHandler(
      enabled = state.selectedNodeId != null,
      onBack = intents.closeNodePreview
    )
    ScreenScaffold(state = scaffoldState) {
      val scope = rememberCoroutineScope()
      val hapticFeedback = LocalHapticFeedback.current
      val canvasState = rememberAtlasCanvasState(
        graph = state.chronology.graph,
        levels = ChronologyLevels,
        initialLevel = GraphLevel.LOD0
      )
      val ceremonyState = rememberMergeCeremonyState()
      val minimapLabel = remember(canvasState) {
        // Время есть только у эпизода: над точкой на линии пузырь остаётся без подписи, а не берёт
        // её у соседа.
        derivedStateOf { (canvasState.centralNode.value as? GraphNode.Episode)?.time }
      }
      Box(modifier = Modifier.fillMaxSize()) {
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          state = canvasState,
          ceremony = ceremonyState,
          gesturesEnabled = { state.selectedNodeId == null },
          node = { graphNode, accent, level ->
            GraphNode(
              scope = scope,
              hapticFeedback = hapticFeedback,
              level = level,
              accent = accent,
              node = graphNode,
              selectedNode = state.selectedNodeId,
              mergeCeremonyState = ceremonyState,
              chronology = state.chronology,
              onClick = intents.selectNode
            )
          },
          overlay = { onBoundsChanged ->
            Column(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              val flightSpec = AppTheme.motion.largeTween<Float>()
              if (state.debugOverlayAvailable && state.debugOverlayVisible) {
                DebugOverlay(
                  state = canvasState,
                  levelLabelOf = { level -> level.toDebugLabel() },
                  onBoundsChanged = onBoundsChanged
                )
              }
              CameraButtons(
                modifier = Modifier.align(Alignment.End),
                onStart = { canvasState.flyTo(scope, flightSpec) { centres.first() } },
                onFront = { canvasState.flyTo(scope, flightSpec) { centres.last() } },
                onBoundsChanged = onBoundsChanged
              )
              GraphMinimap(
                span = canvasState.viewportSpan,
                marks = canvasState.laneMarks,
                label = minimapLabel,
                onScrub = { fraction -> canvasState.scrubTo(fraction) },
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
        ScrimEffect(
          visible = state.selectedNodeId != null,
          onFinish = intents.closeNodePreview
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
