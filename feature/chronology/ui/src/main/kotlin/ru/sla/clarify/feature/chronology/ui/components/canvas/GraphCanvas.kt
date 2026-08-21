package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Полотно хронологии: фон, магистраль и слой узлов, по которому можно панорамировать.
 *
 * Камера — это сдвиг содержимого: `экран = полотно + камера`. Её границы считаются по фактически
 * размещённым узлам, а не по объявленному размеру полотна: узел ставится центром в точку дорожки
 * и потому выступает за край на половину своей ширины, а по объявленному размеру этот выступ
 * недостижим прокруткой.
 *
 * Зума и переключения уровней детализации здесь пока нет.
 */
@Composable
internal fun GraphCanvas(
  contentSize: DpSize,
  lanes: IntRange,
  modifier: Modifier = Modifier,
  debugOverlayVisible: Boolean = false,
  content: @Composable GraphScope.() -> Unit
) {
  BoxWithConstraints(modifier = modifier.clipToBounds()) {
    val density = LocalDensity.current
    val declaredBounds = remember(contentSize, density) {
      with(density) { Rect(0f, 0f, contentSize.width.toPx(), contentSize.height.toPx()) }
    }
    var contentBounds by remember(declaredBounds) { mutableStateOf(declaredBounds) }
    // null — камера ещё не сдвигалась: тогда она встаёт вплотную к началу истории.
    var camera by remember { mutableStateOf<Offset?>(null) }

    val cameraX = cameraRange(contentBounds.left, contentBounds.right, constraints.maxWidth.toFloat())
    val cameraY = cameraRange(contentBounds.top, contentBounds.bottom, constraints.maxHeight.toFloat())
    val cameraOffset = Offset(
      x = (camera?.x ?: cameraX.endInclusive).coerceIn(cameraX),
      y = (camera?.y ?: cameraY.endInclusive).coerceIn(cameraY)
    )

    // Счётчики для отладочной панели: без них не отличить «жест не дошёл» от «камера упёрлась».
    var lastDrag by remember { mutableStateOf(Offset.Zero) }
    var dragCount by remember { mutableIntStateOf(0) }

    Box(
      // Жест висит на всём вьюпорте, а не на слое узлов: слой узлов ограничен размером полотна,
      // и панорамирование не работало бы там, где полотно до края экрана не достаёт.
      modifier = Modifier
        .matchParentSize()
        .pointerInput(cameraX, cameraY) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            val current = camera ?: Offset(cameraX.endInclusive, cameraY.endInclusive)
            camera = Offset(
              x = (current.x + dragAmount.x).coerceIn(cameraX),
              y = (current.y + dragAmount.y).coerceIn(cameraY)
            )
            lastDrag = dragAmount
            dragCount++
          }
        }
    ) {
      GraphBackdrop(
        modifier = Modifier.fillMaxSize(),
        camera = cameraOffset,
        lanes = lanes
      )

      val trunkColor = AppTheme.colors.contentTertiary
      val trunkY = GraphGeometry.TrunkY
      val trunkBounds = contentBounds
      Layout(
        // Слой узлов равен вьюпорту, а не полотну. `requiredSize` центрирует содержимое, которое
        // шире входящих ограничений, и полотно уезжало влево на (viewport - content) / 2 мимо
        // камеры: её предел считался от нуля, а полотно начиналось левее. Узлы выходят за границы
        // слоя — это допустимо, слой не обрезает, обрезает вьюпорт снаружи.
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            translationX = cameraOffset.x
            translationY = cameraOffset.y
          }
          .drawBehind {
            // Магистраль начинается в центре начального узла, а не у левого края полотна: слева
            // от начала истории её нечему обозначать. Плашка узла её перекрывает, поэтому линия
            // читается как выходящая из узла вправо.
            val y = trunkY.toPx()
            drawLine(
              color = trunkColor,
              start = Offset(size.width / 2f, y),
              end = Offset(trunkBounds.right, y),
              strokeWidth = TRUNK_WIDTH.toPx()
            )
          },
        content = { GraphScopeInstance.content() }
      ) { measurables, constraints ->
        // Узел меряется свободно: ширину он ограничивает сам, а вьюпорт ему не указ — узел может
        // стоять далеко за правым краем экрана.
        val placeables = measurables.map { it.measure(Constraints()) }
        val positions = measurables.map { it.parentData as? GraphNodePosition }
        // Начало истории встаёт центром в центр экрана: слева от него отступ, а не обрезанная
        // плашка. Сдвиг общий для всех узлов, иначе поедет относительная геометрия графа.
        val leadingX = positions.filterNotNull().minOfOrNull { it.x }
        val leadingShift = if (leadingX == null) 0 else constraints.maxWidth / 2 - leadingX.roundToPx()
        val offsets = placeables.mapIndexed { index, placeable ->
          positions[index]?.let {
            IntOffset(
              x = it.x.roundToPx() + leadingShift - placeable.width / 2,
              y = it.y.roundToPx() - placeable.height / 2
            )
          }
        }
        contentBounds = placedBounds(declaredBounds, placeables, offsets)
        layout(constraints.maxWidth, constraints.maxHeight) {
          offsets.forEachIndexed { index, offset ->
            if (offset != null) placeables[index].place(offset)
          }
        }
      }
    }

    if (debugOverlayVisible) {
      GraphDebugOverlay(
        modifier = Modifier.align(Alignment.BottomStart),
        info = GraphDebugInfo(
          viewportWidth = constraints.maxWidth,
          viewportHeight = constraints.maxHeight,
          declaredBounds = declaredBounds,
          contentBounds = contentBounds,
          cameraMinX = cameraX.start,
          cameraMaxX = cameraX.endInclusive,
          cameraMinY = cameraY.start,
          cameraMaxY = cameraY.endInclusive,
          camera = cameraOffset,
          isCameraMoved = camera != null,
          dragCount = dragCount,
          lastDrag = lastDrag
        )
      )
    }
  }
}

/**
 * Допустимый сдвиг содержимого по одной оси.
 *
 * Чтобы начало содержимого встало у начала экрана, нужен сдвиг `-min`; чтобы конец содержимого
 * встал у конца экрана — `viewport - max`. Если содержимое короче экрана, обе границы схлопываются
 * в одно центрирующее значение: содержимое незачем прижимать к краю, когда оно целиком помещается.
 */
private fun cameraRange(min: Float, max: Float, viewport: Float): ClosedFloatingPointRange<Float> {
  val lower = viewport - max
  val upper = -min
  if (lower <= upper) return lower..upper
  val centered = (viewport - (max - min)) / 2f - min
  return centered..centered
}

/** Объявленное полотно, расширенное выступами узлов за его края. */
private fun placedBounds(
  declared: Rect,
  placeables: List<Placeable>,
  offsets: List<IntOffset?>
): Rect {
  var left = declared.left
  var top = declared.top
  var right = declared.right
  var bottom = declared.bottom
  offsets.forEachIndexed { index, offset ->
    if (offset == null) return@forEachIndexed
    val placeable = placeables[index]
    left = min(left, offset.x.toFloat())
    top = min(top, offset.y.toFloat())
    right = max(right, (offset.x + placeable.width).toFloat())
    bottom = max(bottom, (offset.y + placeable.height).toFloat())
  }
  return Rect(left, top, right, bottom)
}

private val TRUNK_WIDTH: Dp = 2.dp
