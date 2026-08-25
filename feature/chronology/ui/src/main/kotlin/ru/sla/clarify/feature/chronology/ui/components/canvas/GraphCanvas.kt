package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.TransformOrigin
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
 * считает [graphPlacementOf]; камеру и результат раскладки держит [GraphCanvasState]; события
 * пальцев разбирает [detectCameraGestures].
 *
 * Масштаб — свойство камеры, а не раскладки: он применяется слоем и потому не стоит ни измерения,
 * ни рекомпозиции. Переключения уровней детализации по порогам масштаба пока нет — узлы на всех
 * масштабах рисуются одни и те же, и на нижней границе диапазона текст становится нечитаемым по
 * построению.
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
  node: @Composable (id: GraphNode.Id) -> Unit,
  overlay: @Composable BoxScope.(onBoundsChanged: (Rect) -> Unit) -> Unit = { }
) {
  val telemetry = state.telemetry
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

  SideEffect { telemetry.onCanvasComposition() }
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
        detectCameraGestures(
          isBlocked = { position -> currentPanelBounds.contains(position) },
          onTouch = { currentState.stopFling() },
          onTransform = { focus, pan, zoom ->
            // Масштаб ложится первым: он меняет и границы камеры, и то, куда попадёт та же точка
            // экрана, — а сдвиг центроида поверх этого уже обычный шаг протяжки. Два клампа за
            // событие вместо одного здесь ничего не стоят: незажатый сдвиг никуда не копится,
            // потому что камера хранится уже зажатой.
            if (zoom != 1f) {
              currentState.zoom(focus = focus, change = zoom)
            }
            if (pan != Offset.Zero) {
              currentState.pan(pan)
            }
          },
          onRelease = { velocity ->
            currentState.fling(scope = flingScope, velocity = velocity, decay = currentDecay)
          }
        )
      }
  ) {
    GraphBackdrop(
      modifier = Modifier.fillMaxSize(),
      state = state
    )
    GraphNodesLayer(
      state = state,
      nodes = nodes,
      node = node
    )
    overlay { bounds -> panelBounds = bounds }
  }
}

/**
 * Слой узлов: плашки, связи между ними и камера, двигающая их все разом.
 *
 * Слой равен вьюпорту, а не полотну: `requiredSize` центрирует содержимое шире входящих
 * ограничений, и полотно уезжало бы мимо камеры. Узлы выходят за границы слоя — слой не обрезает,
 * обрезает вьюпорт снаружи.
 *
 * Жест сюда не приходит: он висит на всём вьюпорте, потому что полотно не всегда достаёт до края
 * экрана и панорамирование не работало бы там, где узлов нет.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param nodes узлы, из которых строится содержимое; приходят параметром, а не читаются из [state],
 *   чтобы композиция и измерение одного кадра видели один и тот же список
 * @param modifier модификатор слоя
 * @param node содержимое узла с данным `id`
 */
@Composable
private fun GraphNodesLayer(
  state: GraphCanvasState,
  nodes: List<GraphNode>,
  modifier: Modifier = Modifier,
  node: @Composable (id: GraphNode.Id) -> Unit
) {
  val telemetry = state.telemetry
  val edgeColor = AppTheme.colors.contentTertiary
  val statusBar = WindowInsets.statusBars
  val navigationBar = WindowInsets.navigationBars
  Layout(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer {
        // Камера и масштаб читаются здесь, а не в композиции: кадр панорамирования и кадр пинча
        // обновляют только свойства слоя — ни рекомпозиции, ни повторного измерения, ни новых
        // модификаторов. Узлы при этом не перевёрстываются: масштаб применяется к готовому списку
        // команд рисования, поэтому текст перерисовывается в конечном размере и остаётся резким.
        telemetry.onLayerUpdate()
        // Начало координат — угол вьюпорта, а не его центр, и это не оформление: только так
        // выполняется `экран = полотно · scale + камера`, из которого выведены и диапазон камеры,
        // и удержание точки под пальцами. С центром пришлось бы вносить размер вьюпорта в обе
        // формулы дважды.
        transformOrigin = TransformOrigin(0f, 0f)
        val scale = state.scale.value
        scaleX = scale
        scaleY = scale
        val camera = state.offset.value
        translationX = camera.x
        translationY = camera.y
      }
      .drawBehind {
        // Связи рисуются внутри того же слоя, поэтому масштабируются вместе с плашками: на 0.4×
        // линия истончается до 0.8 dp, на 2.5× толстеет до пяти. Одно правило на весь слой проще
        // двух; если тонкая линия потеряется на устройстве, обратный ход — поделить толщину на
        // масштаб прямо здесь.
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
      density = this,
      statusBar = statusBar.getTop(this).toFloat(),
      navigationBar = navigationBar.getBottom(this).toFloat(),
      viewportSize = IntSize(constraints.maxWidth, constraints.maxHeight),
      nodes = nodes,
      nodeSizes = placeables.fastMap { IntSize(it.width, it.height) }
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
      telemetry.onPlacement()
      placeables.fastForEachIndexed { index, placeable -> placeable.place(placement.nodes[index]) }
    }
  }
}
