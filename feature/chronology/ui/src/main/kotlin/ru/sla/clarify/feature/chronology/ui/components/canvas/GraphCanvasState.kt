package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

/**
 * Состояние полотна для графа из [nodes].
 *
 * Смена набора узлов пересобирает состояние: положение камеры привязано к содержимому, и держать
 * его от прежнего графа бессмысленно.
 *
 * @param nodes узлы в хронологическом порядке
 * @return состояние, живущее до следующей смены [nodes]
 */
@Composable
internal fun rememberGraphCanvasState(nodes: List<GraphNode>): GraphCanvasState {
  return remember(nodes) { GraphCanvasState(nodes) }
}

/**
 * Камера полотна и результат его последней раскладки.
 *
 * Разделение обязанностей: [GraphGeometry] знает, где узлы стоят на полотне, состояние — где
 * полотно стоит относительно экрана и что получилось после измерения, композабл не считает ничего.
 *
 * Про запись из фазы измерения. Размещения, границы содержимого и рёбра лежат в **обычных**, не
 * снапшотных полях, а заполняет их [onMeasure]. Так сделано намеренно: запись снапшот-состояния из
 * measure стоит отложенного оповещения и лишнего прохода композиции с измерением, а границы здесь
 * нужны только фазе рисования и обработчику жеста — и та, и другой всегда идут после измерения,
 * поэтому читают уже свежее значение. Обратной связи «раскладка → состояние → раскладка» не
 * возникает.
 *
 * [offset] и [edges] предназначены для чтения внутри `graphicsLayer` и `drawBehind`, а не в
 * композиции: тогда кадр панорамирования обновляет только свойства слоя — ни рекомпозиции, ни
 * повторного измерения.
 *
 * @param nodes узлы в хронологическом порядке
 */
@Stable
internal class GraphCanvasState(val nodes: List<GraphNode>) {

  private val geometry = GraphGeometry(nodes)

  // Сдвиг камеры без клампа: кламп накладывается на чтении. Хранить уже ограниченное значение
  // означало бы потерять то, что понадобится для оттяжки за край и для затухания инерции.
  private var rawOffsetX by mutableFloatStateOf(0f)
  private var rawOffsetY by mutableFloatStateOf(0f)

  // Флаг, а не сравнение сдвига с границей: композиция читает его через отладочную панель, а
  // значение, выведенное из сдвига, подписало бы её на покадровые изменения.
  private var isMoved by mutableStateOf(false)

  private var viewport = IntSize.Zero
  private var contentBounds = Rect.Zero
  private var placements: List<IntOffset> = emptyList()
  private var laneEdges: List<GraphEdge> = emptyList()

  // Телеметрия отладочной панели. Ревизия — единственная снапшот-запись из measure, и она
  // происходит только при включённом тогле: панель в композиции иначе не узнает, что раскладка
  // сменилась. Выключенный тогл не стоит ничего.
  private var isTelemetryEnabled = false
  private var layoutRevision by mutableIntStateOf(0)
  private var dragCount by mutableIntStateOf(0)
  private var lastDrag by mutableStateOf(Offset.Zero)

  /**
   * Сдвиг содержимого относительно экрана, уже ограниченный содержимым.
   *
   * Пока камеру не двигали, она стоит вплотную к началу истории. Читать внутри `graphicsLayer`.
   *
   * @return сдвиг в пикселях
   */
  fun offset(): Offset {
    val rangeX = panRangeOf(contentBounds.left, contentBounds.right, viewport.width.toFloat())
    val rangeY = panRangeOf(contentBounds.top, contentBounds.bottom, viewport.height.toFloat())
    return Offset(
      x = if (isMoved) rawOffsetX.coerceIn(rangeX) else rangeX.endInclusive,
      y = if (isMoved) rawOffsetY.coerceIn(rangeY) else rangeY.endInclusive
    )
  }

  /**
   * Связи между соседними узлами каждой дорожки. Читать внутри `drawBehind`.
   *
   * @return отрезки в координатах полотна
   */
  fun edges(): List<GraphEdge> {
    return laneEdges
  }

  /**
   * Двигает камеру на [delta].
   *
   * @param delta сдвиг в пикселях экрана
   */
  fun pan(delta: Offset) {
    if (!isMoved) {
      val resting = offset()
      rawOffsetX = resting.x
      rawOffsetY = resting.y
      isMoved = true
    }
    rawOffsetX += delta.x
    rawOffsetY += delta.y
    if (isTelemetryEnabled) {
      dragCount++
      lastDrag = delta
    }
  }

  /**
   * Принимает результат измерения: считает размещения узлов, границы содержимого и рёбра.
   *
   * Вся арифметика раскладки собрана здесь, а не в композабле: размеры узлов известны только после
   * измерения, а всё остальное даёт [GraphGeometry] из модели.
   *
   * @param viewportSize размер видимой области
   * @param nodeSizes измеренные размеры узлов, в порядке [nodes]
   * @param density плотность экрана для перевода координат полотна в пиксели
   */
  fun onMeasure(viewportSize: IntSize, nodeSizes: List<IntSize>, density: Density) {
    viewport = viewportSize
    val offsetsX = geometry.offsetsX()
    // Начало истории встаёт центром в центр экрана: слева от него отступ, а не обрезанная плашка.
    // Сдвиг привязан к первому узлу модели, а не к самому левому из размещённых: иначе догрузка
    // истории или виртуализация уводили бы весь граф в сторону.
    val leadingShift = with(density) {
      viewportSize.width / 2f - (offsetsX.firstOrNull() ?: 0.dp).toPx()
    }
    placements = nodeSizes.mapIndexed { index, size ->
      with(density) {
        IntOffset(
          x = (offsetsX[index].toPx() + leadingShift).toInt() - size.width / 2,
          y = geometry.laneYOf(nodes[index].lane).toPx().toInt() - size.height / 2
        )
      }
    }
    contentBounds = boundsOf(placements, nodeSizes)
    laneEdges = edgesOf(placements, nodeSizes)
    if (isTelemetryEnabled) {
      layoutRevision++
    }
  }

  /**
   * Левый верхний угол узла на полотне.
   *
   * @param index номер узла в [nodes]
   * @return смещение для размещения
   */
  fun placementOf(index: Int): IntOffset {
    return placements.getOrElse(index) { IntOffset.Zero }
  }

  /**
   * Включает сбор телеметрии для отладочной панели.
   *
   * @param enabled собирать ли счётчики и ревизию раскладки
   */
  fun setTelemetryEnabled(enabled: Boolean) {
    isTelemetryEnabled = enabled
  }

  /**
   * Снимок для отладочной панели.
   *
   * Чтение ревизии здесь не декоративно: оно подписывает панель на смену раскладки, значения
   * которой лежат вне снапшот-состояния.
   *
   * @return снимок камеры и последней раскладки
   */
  fun debugInfo(): GraphDebugInfo {
    val revision = layoutRevision
    val camera = offset()
    return GraphDebugInfo(
      viewportWidth = viewport.width,
      viewportHeight = viewport.height,
      contentBounds = contentBounds,
      camera = camera,
      isCameraMoved = isMoved,
      layoutRevision = revision,
      nodeCount = nodes.size,
      edgeCount = laneEdges.size,
      dragCount = dragCount,
      lastDrag = lastDrag
    )
  }

  /**
   * Границы содержимого: объединение прямоугольников всех узлов.
   *
   * Единственный источник истины о протяжённости полотна — сами узлы. Объявленный извне размер
   * полотна был бы вторым, и они разошлись бы при первой же реальной переписке.
   *
   * @param placements левые верхние углы узлов
   * @param sizes размеры узлов
   * @return объединение, пустое при отсутствии узлов
   */
  private fun boundsOf(placements: List<IntOffset>, sizes: List<IntSize>): Rect {
    if (placements.isEmpty()) {
      return Rect.Zero
    }
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    placements.forEachIndexed { index, placement ->
      val size = sizes[index]
      if (placement.x < left) left = placement.x.toFloat()
      if (placement.y < top) top = placement.y.toFloat()
      if (placement.x + size.width > right) right = (placement.x + size.width).toFloat()
      if (placement.y + size.height > bottom) bottom = (placement.y + size.height).toFloat()
    }
    return Rect(left, top, right, bottom)
  }

  /**
   * Отрезки связей: между соседними по времени узлами одной дорожки.
   *
   * Ребро существует только там, где есть что связывать, поэтому после последнего узла дорожки его
   * нет и линия не уходит в пустоту. Отрезок живёт строго в зазоре между плашками: узел бывает
   * полупрозрачным, и линия под ним просвечивала бы.
   *
   * Дорожка сейчас отождествляется с ветвью. Когда появится переиспользование дорожки после
   * слияния, группировать придётся по идентификатору ветви, иначе две несвязанные ветви получат
   * ложное ребро.
   *
   * @param placements левые верхние углы узлов
   * @param sizes размеры узлов
   * @return отрезки в координатах полотна
   */
  private fun edgesOf(placements: List<IntOffset>, sizes: List<IntSize>): List<GraphEdge> {
    val previousByLane = HashMap<Int, Int>()
    val result = mutableListOf<GraphEdge>()
    placements.indices.forEach { index ->
      val lane = nodes[index].lane
      val previous = previousByLane.put(lane, index)
      if (previous != null) {
        val startX = (placements[previous].x + sizes[previous].width).toFloat()
        val endX = placements[index].x.toFloat()
        if (endX > startX) {
          result += GraphEdge(
            startX = startX,
            endX = endX,
            y = placements[previous].y + sizes[previous].height / 2f
          )
        }
      }
    }
    return result
  }
}
