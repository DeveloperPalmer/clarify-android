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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.atlas.debug.DebugOverlay
import ru.sla.atlas.ui.rememberAtlasCanvasState
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.CameraButtons
import ru.sla.clarify.feature.chronology.ui.components.canvas.ChronologyLevels
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphMinimap
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberMergeCeremonyState
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
    // Открыта карточка — закрывается она; иначе закрывается экран, и этим занимается уже хост флоу.
    BackHandler(
      enabled = state.selectedNodeId != null,
      onBack = intents.closeNodePreview
    )
    ScreenScaffold(state = scaffoldState) {
      val canvasState = rememberAtlasCanvasState(
        graph = state.chronology.graph,
        levels = ChronologyLevels,
        initialLevel = GraphLevel.LOD0
      )
      val scope = rememberCoroutineScope()
      val hapticFeedback = LocalHapticFeedback.current
      val ceremonyState = rememberMergeCeremonyState()
      // Церемония доигрывает после того, как композиция чипа уже могла уйти, поэтому scope свой.
      val ceremonyScope = rememberCoroutineScope()
      // Подпись пузыря мини-карты. Собирается здесь, потому что дату знает экран, а какой узел под
      // центром — полотно; отдаётся `State`, чтобы прочитал её лист, а не тело экрана: чтение
      // прямо тут пересобирало бы лямбды полотна при каждой смене узла под камерой.
      val minimapLabel = remember(canvasState) {
        // Время есть только у эпизода: над точкой на линии пузырь остаётся без подписи, а не берёт
        // её у соседа.
        derivedStateOf { (canvasState.centralNode.value as? GraphNode.Episode)?.time }
      }
      // Перелёт доигрывает после того, как кнопка могла уйти с экрана вместе с панелью, поэтому
      // scope берётся у экрана, а не у неё. Кривая — из темы: 400 мс, затухание без разгона.
      val flightScope = rememberCoroutineScope()
      val flightSpec = AppTheme.motion.largeTween<Float>()
      Box(modifier = Modifier.fillMaxSize()) {
        // Скругление плашки берётся из того же токена, которым она нарисована, и умножается на
        // масштаб: на 2.5× нарисованный радиус равен сорока, и морф, стартовавший с шестнадцати,
        // начался бы с чужими углами.
        val density = LocalDensity.current
        val episodeCorner = with(density) {
          AppTheme.shapes.round16.topStart.toPx(Size.Zero, this).toDp()
        }
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          state = canvasState,
          ceremony = ceremonyState,
          // Пока карточка открыта, полотно жестов не берёт вовсе: прямоугольник, из которого вырос
          // морф, заморожен, и уехавшая под скримом камера сделала бы обратный морф ложью.
          gesturesEnabled = { state.selectedNodeId == null },
          node = { graphNode, accent, level ->
            GraphNode(
              chronology = state.chronology,
              selectedNode = state.selectedNodeId,
              node = graphNode,
              level = level,
              accent = accent,
              mergeCeremonyState = ceremonyState,
              scope = scope,
              hapticFeedback = hapticFeedback,
              onClick = intents.selectNode
            )
          },
          overlay = { onBoundsChanged ->
            // Инструменты стоят колонкой у нижнего края, мини-карта снизу: колонка растёт вверх,
            // поэтому появление панели не двигает мини-карту, а §11.1 требует, чтобы полоса стояла
            // на месте всегда.
            Column(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              if (state.debugOverlayAvailable && state.debugOverlayVisible) {
                DebugOverlay(
                  state = canvasState,
                  levelLabelOf = { level -> level.toDebugLabel() },
                  onBoundsChanged = onBoundsChanged
                )
              }
              CameraButtons(
                // Кнопки стоят в той же колонке, что панель и мини-карта, поэтому отступ «над
                // мини-картой» из §11.1 получается сам и не зависит от её высоты числом.
                modifier = Modifier.align(Alignment.End),
                // Куда лететь, полотно не знает: исток — самое начало переписки, фронт — последнее
                // по времени, что в ней есть. Оба спрашиваются у раскладки каждый кадр, поэтому
                // пришедшее во время перелёта сообщение уводит фронт вместе с собой.
                onStart = { canvasState.flyTo(flightScope, flightSpec) { centres.first() } },
                onFront = { canvasState.flyTo(flightScope, flightSpec) { centres.last() } },
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
        // Скрим накрывает и шапку: иначе стрелка «назад» осталась бы живой и уводила бы с экрана
        // вместо того, чтобы закрыть карточку.
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
