package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Полотно хронологии: фон, узлы графа и связи между ними, по которому можно панорамировать.
 *
 * Композабл здесь ничего не считает — только композирует, принимает жест и рисует. Где узлы стоят,
 * знает [GraphGeometry]; где стоит полотно и что получилось после измерения — [GraphCanvasState].
 *
 * Зума и переключения уровней детализации пока нет.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param modifier модификатор корня полотна
 * @param debugOverlayVisible показывать ли отладочную панель камеры
 * @param node содержимое узла с данным `id`
 */
@Composable
internal fun GraphCanvas(
  state: GraphCanvasState,
  modifier: Modifier = Modifier,
  debugOverlayVisible: Boolean = false,
  node: @Composable (id: GraphNode.Id) -> Unit
) {
  state.setTelemetryEnabled(debugOverlayVisible)

  val edgeColor = AppTheme.colors.contentTertiary

  Box(
    // Жест висит на всём вьюпорте, а не на слое узлов: полотно не всегда достаёт до края экрана,
    // и панорамирование не работало бы там, где его нет.
    modifier = modifier
      .clipToBounds()
      .pointerInput(state) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          state.pan(dragAmount)
        }
      }
  ) {
    GraphBackdrop(
      modifier = Modifier.fillMaxSize(),
      state = state
    )

    Layout(
      // Слой узлов равен вьюпорту, а не полотну: `requiredSize` центрирует содержимое шире
      // входящих ограничений, и полотно уезжало бы мимо камеры. Узлы выходят за границы слоя —
      // слой не обрезает, обрезает вьюпорт снаружи.
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          // Камера читается здесь, а не в композиции: кадр панорамирования обновляет только
          // свойства слоя — ни рекомпозиции, ни повторного измерения, ни новых модификаторов.
          val camera = state.offset()
          translationX = camera.x
          translationY = camera.y
        }
        .drawBehind {
          state.edges().fastForEach { edge ->
            drawLine(
              color = edgeColor,
              start = Offset(edge.startX, edge.y),
              end = Offset(edge.endX, edge.y),
              strokeWidth = EDGE_WIDTH.toPx()
            )
          }
        },
      content = {
        state.nodes.fastForEach { graphNode ->
          key(graphNode.id.value) {
            node(graphNode.id)
          }
        }
      }
    ) { measurables, constraints ->
      // Узел меряется свободно: ширину он ограничивает сам, а вьюпорт ему не указ — узел может
      // стоять далеко за правым краем экрана.
      val placeables = measurables.fastMap { it.measure(Constraints()) }
      state.onMeasure(
        viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight),
        nodeSizes = placeables.fastMap { IntSize(it.width, it.height) },
        density = this
      )
      layout(constraints.maxWidth, constraints.maxHeight) {
        placeables.fastForEachIndexed { index, placeable ->
          placeable.place(state.placementOf(index))
        }
      }
    }

    if (debugOverlayVisible) {
      GraphDebugOverlay(
        modifier = Modifier.align(Alignment.BottomStart),
        info = state.debugInfo()
      )
    }
  }
}
