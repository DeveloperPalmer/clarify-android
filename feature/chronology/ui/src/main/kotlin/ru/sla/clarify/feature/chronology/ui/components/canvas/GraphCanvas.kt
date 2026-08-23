package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Полотно хронологии: фон, узлы графа и связи между ними, по которому можно панорамировать.
 *
 * Композабл здесь ничего не считает — только композирует, принимает жест и рисует. Где узлы стоят,
 * считает [graphPlacementOf]; камеру и результат раскладки держит [GraphCanvasState].
 *
 * Зума и переключения уровней детализации пока нет.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param modifier модификатор корня полотна
 * @param overlay что нарисовать поверх полотна: панель, линейка, что угодно. Слот получает
 *   обработчик, которым содержимое объявляет занятую им зону, — жест, начатый в этой зоне, до
 *   камеры не доходит. Полотно при этом не знает, что именно там лежит
 * @param node содержимое узла с данным `id`; обязано выпускать ровно один элемент раскладки —
 *   полотно ставит плашки по одной на узел и считает их по позиции, а не по идентификатору
 */
@Composable
internal fun GraphCanvas(
  state: GraphCanvasState,
  modifier: Modifier = Modifier,
  overlay: @Composable BoxScope.(onBoundsChanged: (Rect) -> Unit) -> Unit = { },
  node: @Composable (id: GraphNode.Id) -> Unit
) {
  // Жест не пересоздаётся при смене состояния: ключ `Unit` держит обработчик живым, а свежий
  // экземпляр приходит через rememberUpdatedState. Иначе первое же входящее сообщение отменяло бы
  // драг под пальцем.
  val currentState by rememberUpdatedState(state)
  // Спека тоже обновляется через rememberUpdatedState: `pointerInput(Unit)` не пересоздаётся, и
  // смена плотности иначе заморозила бы внутри жеста кривую от старого экрана.
  val currentDecay by rememberUpdatedState(AppTheme.motion.flingDecay<Float>())
  // Затухание доигрывает после того, как корутина жеста уже отменена, поэтому scope нужен свой.
  val flingScope = rememberCoroutineScope()
  // Зона, занятая тем, что лежит поверх полотна, в координатах этого же Box: полотно ловит жест на
  // всём вьюпорте и по ней отличает палец, положенный на инструмент, от пальца на графе. Зону
  // объявляет и снимает само содержимое слота — полотно её не вычисляет и о её природе не знает.
  //
  // Поглощать жесты внутри панели нельзя, хотя это выглядело бы проще: `clickable` потребляет лишь
  // нажатие с отпусканием, а камеру двигает протяжка, и полотно принимает даже потреблённое
  // нажатие. Потреблять же сами движения — значит убить листание панели: детекторы жестов
  // проверяют потребление ещё и в Final-проходе, и вложенный пейджер отменяется.
  var panelBounds by remember { mutableStateOf(Rect.Zero) }
  val currentPanelBounds by rememberUpdatedState(panelBounds)
  val telemetry = state.telemetry
  SideEffect { telemetry.onCanvasComposition() }
  val edgeColor = AppTheme.colors.contentTertiary
  // Узлы снимаются один раз и уходят и в содержимое, и в измерение. Читать их в measure заново
  // нельзя: состояние подменяется из `SideEffect`, то есть уже после этой композиции, но ещё до
  // измерения того же кадра, — и измерение получило бы новый список к старым measurable'ам.
  val nodes = state.nodes

  Box(
    // Жест висит на всём вьюпорте, а не на слое узлов: полотно не всегда достаёт до края экрана,
    // и панорамирование не работало бы там, где его нет.
    modifier = modifier
      .clipToBounds()
      .pointerInput(Unit) {
        // Трекер живёт со всем обработчиком: ключ `Unit` держит его между жестами, а сбрасывается
        // он на каждом касании. Скорость foundation не считает — она отдаёт только up-событие.
        val tracker = VelocityTracker()
        // Решение принимается один раз, на касании, и держится весь жест: палец, ушедший с панели
        // на полотно, не должен посреди движения начать таскать камеру.
        var startedOnPanel = false
        detectDragGestures(
          orientationLock = null,
          onDragStart = { down, _, _ ->
            startedOnPanel = currentPanelBounds.contains(down.position)
            if (startedOnPanel) return@detectDragGestures
            currentState.stopFling()
            tracker.resetTracking()
            tracker.addPointerInputChange(down)
          },
          onDragEnd = { up ->
            if (startedOnPanel) return@detectDragGestures
            tracker.addPointerInputChange(up)
            // Кламп по осям, как у платформы: у трекера полиномиальная подгонка, и дрожание перед
            // отпусканием умеет отдать десятки тысяч px/s, а пролёт растёт как v^1.736.
            val maximum = viewConfiguration.maximumFlingVelocity
            currentState.fling(
              scope = flingScope,
              velocity = tracker.calculateVelocity(Velocity(maximum, maximum)),
              decay = currentDecay
            )
          },
          onDragCancel = { tracker.resetTracking() },
          onDrag = { change, dragAmount ->
            if (startedOnPanel) return@detectDragGestures
            // `change.consume()` здесь не нужен: перегрузка с `orientationLock` консьюмит сама —
            // и на пересечении слопа, и на каждом последующем событии.
            tracker.addPointerInputChange(change)
            currentState.pan(dragAmount)
          }
        )
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
          telemetry.onLayerUpdate()
          val camera = state.offset.value
          translationX = camera.x
          translationY = camera.y
        }
        .drawBehind {
          telemetry.onEdgeDraw()
          state.edges.value.fastForEach { edge ->
            drawLine(
              color = edgeColor,
              start = Offset(edge.startX, edge.y),
              end = Offset(edge.endX, edge.y),
              strokeWidth = EDGE_WIDTH.toPx()
            )
          }
        },
      content = {
        nodes.fastForEach { graphNode ->
          key(graphNode.id.value) {
            SideEffect { telemetry.onNodeComposition() }
            node(graphNode.id)
          }
        }
      }
    ) { measurables, constraints ->
      // Узел меряется свободно: ширину он ограничивает сам, а вьюпорт ему не указ — узел может
      // стоять далеко за правым краем экрана.
      val placeables = measurables.fastMap { it.measure(Constraints()) }
      val placement = state.layout(
        nodes = nodes,
        viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight),
        nodeSizes = placeables.fastMap { IntSize(it.width, it.height) },
        density = this
      )
      layout(constraints.maxWidth, constraints.maxHeight) {
        telemetry.onPlacement()
        placeables.fastForEachIndexed { index, placeable ->
          placeable.place(placement.nodes[index])
        }
      }
    }

    overlay { bounds -> panelBounds = bounds }
  }
}
