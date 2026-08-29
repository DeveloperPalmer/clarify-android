package ru.sla.clarify.feature.chronology.ui.components.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeSelection
import ru.sla.clarify.uikit.animation.SharedContainer
import kotlin.math.roundToInt

/**
 * Морф узла в превью-карточку: обе половины и то, чем они связаны (§11.2 брифа).
 *
 * **Почему источник — якорь, а не сам узел.** `SharedContainer` берёт целевые границы у
 * lookahead-позиции, а та складывает только позиции размещения и преобразований `graphicsLayer` не
 * видит вовсе. Камера полотна — это как раз `graphicsLayer`, поэтому морф, повешенный на узел
 * напрямую, стартовал бы из **мировой** координаты: на демо-графе это до двадцати экранов правее и,
 * вдобавок, из немасштабированного размера — 200 × 72 dp вместо нарисованных 500 × 180 dp на 2.5×.
 * Поэтому источник кладётся вне слоя камеры, а прямоугольник ему считает полотно.
 *
 * **Почему якорь пустой и прозрачный.** Во время перехода общий элемент рисует оба конца в оверлее
 * общей области, и поверхность, которую видит глаз, — поверхность карточки, ужатая до плашки. Своей
 * заливки якорю не нужно, а копией узла он быть не может: при масштабе, отличном от единицы,
 * раскладка копии врала бы о её нарисованном размере. Пустой прямоугольник врать не умеет —
 * содержимого у него нет, масштабировать нечего.
 *
 * **Почему нужен кадр форы.** Общий элемент связывает два конца только тогда, когда один из них уже
 * стоял на экране, — а узел под карточкой появляется вместе с ней. Поэтому [open] переворачивается
 * не в том кадре, где пришёл выбор: сперва композируется и размещается якорь, и лишь следующим
 * кадром видимость меняется местами. Корректность от точного числа кадров не зависит — важно, что
 * [open] может стать истинным только в композиции, которая уже видела [shown].
 *
 * @param selection выбранный узел или `null`, если карточка закрыта
 * @param modifier модификатор области, в которой живут обе половины; обязан совпадать по координатам
 *   с полотном — прямоугольник якоря считан в его системе
 */
@Composable
internal fun NodePreviewMorph(
  selection: GraphNodeSelection?,
  modifier: Modifier = Modifier
) {
  // Последний выбор переживает своё снятие и не снимается вовсе: обратный морф обязан доиграть уже
  // после того, как состояние экрана про выбранный узел забыло. Оставшийся якорь ничего не рисует и
  // жестов не берёт, а следующий тап просто заменит его собой.
  var shown by remember { mutableStateOf<GraphNodeSelection?>(null) }
  var open by remember { mutableStateOf(false) }
  LaunchedEffect(selection) {
    if (selection != null) {
      shown = selection
    }
  }
  LaunchedEffect(shown, selection) {
    open = shown != null && selection != null
  }
  val frame = shown ?: return
  Box(modifier = modifier.fillMaxSize()) {
    NodeMorphAnchor(selection = frame, visible = !open)
    NodePreviewCard(
      // По центру экрана, а не рядом с узлом: узел бывает где угодно, включая самый край, и центр —
      // единственное положение, которое от его места не зависит.
      modifier = Modifier
        .align(Alignment.Center)
        .padding(horizontal = 12.dp),
      preview = frame.preview,
      visible = open,
      morphedCorner = frame.corner
    )
  }
}

/**
 * Источник морфа: пустой прямоугольник ровно там, где нарисована плашка.
 *
 * Ни содержимого, ни заливки: он существует ради границ, из которых растёт карточка. Размер и
 * положение приходят замороженными — камера под открытой карточкой стоит, потому что полотно на это
 * время жестов не берёт.
 *
 * @param selection выбранный узел: его прямоугольник и скругление
 * @param visible виден ли якорь; истинно ровно тогда, когда карточка закрыта
 */
@Composable
private fun NodeMorphAnchor(
  selection: GraphNodeSelection,
  visible: Boolean
) {
  val bounds = selection.bounds
  val size = with(LocalDensity.current) { bounds.width.toDp() to bounds.height.toDp() }
  SharedContainer(
    // Смещение читается в фазе размещения: значение здесь заморожено, но лямбда оставлена ради
    // того, что общий элемент берёт позицию именно с размещения.
    modifier = Modifier.offsetOf(bounds.left, bounds.top),
    key = NODE_PREVIEW_MOTION_KEY,
    visible = visible,
    restingCorner = selection.corner,
    morphedCorner = NODE_PREVIEW_CARD_CORNER
  ) {
    Box(
      modifier = Modifier
        .sharedSurface(color = Color.Transparent)
        .size(width = size.first, height = size.second)
    )
  }
}

/**
 * Смещение в пикселях, читаемое при размещении.
 *
 * Отдельным расширением, потому что `Modifier.offset` с лямбдой требует `IntOffset`, а прямоугольник
 * узла живёт в долях пикселя: округление здесь одно на обе оси и на виду.
 *
 * @param x смещение по горизонтали в пикселях
 * @param y смещение по вертикали в пикселях
 * @return модификатор со смещением
 */
private fun Modifier.offsetOf(x: Float, y: Float): Modifier {
  return offset { IntOffset(x = x.roundToInt(), y = y.roundToInt()) }
}

internal const val NODE_PREVIEW_MOTION_KEY: String = "chronology-node-preview-motion-key"

internal val NODE_PREVIEW_CARD_CORNER: Dp = 24.dp

/**
 * Когда проявляется содержимое карточки: во второй половине морфа.
 *
 * Те же доли, что у карточки merge request: два морфа в одном приложении обязаны выглядеть одним
 * приёмом, а не двумя похожими.
 */
internal val NODE_PREVIEW_CARD_REVEAL_WINDOW: ClosedFloatingPointRange<Float> = 0.2f..1f
