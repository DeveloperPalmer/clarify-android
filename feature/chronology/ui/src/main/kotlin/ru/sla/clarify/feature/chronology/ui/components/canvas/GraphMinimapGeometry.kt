package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import ru.sla.atlas.entity.Branch
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphLaneMark
import ru.sla.clarify.feature.chronology.ui.entity.GraphViewportSpan

/*
 * Перевод между камерой полотна и полосой мини-карты: где камера стоит по времени, какая камера
 * покажет запрошенное место и где на полосе стоят засечки веток.
 *
 * Зависимость односторонняя: здесь знают, как устроена камера, а камера про полосу не знает вовсе.
 * Поэтому тема и вынесена отдельно от `GraphCamera.kt`: правка камеры требует прочитать этот файл,
 * обратное неверно.
 *
 * Мир полосы — **отрезок центров крайних плашек**, а не границы содержимого. Разница не
 * косметическая: камера упирается тогда, когда в центре экрана стоит центр крайней плашки, и мир,
 * растянутый до краёв полей полотна, отдавал бы 2.5 % полосы у каждого края под ход, которого у
 * камеры нет. Отрезок центров от масштаба не зависит, поэтому засечки при зуме остаются на месте.
 *
 * Остаточное расхождение всё же есть, и оно записано числом: выше масштаба 1.256 диапазон камеры
 * задаёт уже не политика центров, а политика краёв содержимого, и последние 4.5 dp хода камеры на
 * 2.5× рамка стоит. Убрать и это можно, только сделав миром достижимую область камеры, — но она
 * зависит от масштаба, и засечки поехали бы под пальцем во время зума.
 */

/**
 * Точка полотна, оказавшаяся в центре экрана.
 *
 * Одно определение на всех: из него выводится и положение рамки ([viewportSpanOf]), и узел под
 * подписью пузыря, а [scrubbedCameraXOf] — обратная к нему функция. Второе определение того же
 * понятия разошлось бы с первым при первой правке любой из трёх.
 *
 * @param camera сдвиг содержимого в пикселях экрана
 * @param scale масштаб содержимого
 * @param viewport размер видимой области
 * @return координата полотна по оси времени
 */
internal fun centreXOf(camera: Offset, scale: Float, viewport: IntSize): Float {
  if (scale <= 0f) {
    return 0f
  }
  return (viewport.width / 2f - camera.x) / scale
}

/**
 * Где камера стоит по времени и какую долю истории видно.
 *
 * Ширина — честная пропорция «окно к истории», без оглядки на палец: расширять её до пригодного
 * для хвата размера — дело того, кто рисует полосу, а не камеры, см. [widenedSpanOf].
 *
 * @param camera сдвиг содержимого в пикселях экрана
 * @param scale масштаб содержимого
 * @param centreSpan отрезок центров крайних плашек в координатах полотна
 * @param viewport размер видимой области
 * @return положение и ширина видимого; «видно всё» при вырожденной истории и при истории уже экрана
 */
internal fun viewportSpanOf(
  camera: Offset,
  scale: Float,
  centreSpan: ClosedFloatingPointRange<Float>,
  viewport: IntSize
): GraphViewportSpan {
  val world = centreSpan.endInclusive - centreSpan.start
  if (world <= 0f || scale <= 0f || viewport.width <= 0) {
    return GraphViewportSpan.Full
  }
  val width = (viewport.width / (world * scale)).coerceAtMost(1f)
  val centre = centreXOf(camera = camera, scale = scale, viewport = viewport)
  return GraphViewportSpan(
    position = ((centre - centreSpan.start) / world).coerceIn(0f, 1f),
    width = width
  )
}

/**
 * Та же доля, с шириной не меньше [minWidth].
 *
 * Расширяет **инструмент**, а не камера: минимум задан пальцем, а не содержимым. Положение при этом
 * не трогается вовсе — оно доля хода, а не координата, и ход сам укорачивается на ту ширину, что
 * рамка прибавила.
 *
 * Числа, ради которых это заведено: на демо-наборе честная рамка занимает 62 dp полосы при 0.4× и
 * 10 dp при 2.5×. Десять пикселей пальцем не берутся, поэтому выше 0.777× рамка упирается в минимум
 * и перестаёт быть пропорцией. Обратный ход, если хват окажется избыточным, — уменьшить минимум
 * до 24 dp.
 *
 * @param span честная доля видимого
 * @param minWidth наименьшая ширина рамки, долей полосы
 * @return доля с шириной не меньше [minWidth]
 */
internal fun widenedSpanOf(span: GraphViewportSpan, minWidth: Float): GraphViewportSpan {
  if (span.width >= minWidth) {
    return span
  }
  return span.copy(width = minWidth.coerceIn(0f, 1f))
}

/**
 * Где на полосе стоит центр рамки, показывающей [position].
 *
 * Ход рамки — это полоса за вычетом её собственной ширины, поэтому крайние положения точно
 * совпадают с упорами камеры при любом масштабе. Той же функцией размечаются засечки: рамка
 * накрывает засечку центром ровно тогда, когда её ветка оказывается в центре экрана.
 *
 * @param position доля хода камеры по времени
 * @param width ширина рамки, долей полосы
 * @return доля полосы, где стоит центр рамки
 */
internal fun trackCentreOf(position: Float, width: Float): Float {
  return position.coerceIn(0f, 1f) * (1f - width) + width / 2f
}

/**
 * Куда просят увести камеру пальцем, положенным на полосу.
 *
 * Обратная к [trackCentreOf]: палец задаёт середину рамки, а не её край. У самых краёв полосы
 * половина ширины рамки приходится на упор — и это не мёртвый ход, а «уже приехали»: в этих
 * положениях камера действительно стоит на границе своего диапазона.
 *
 * @param x координата касания внутри полосы, в пикселях
 * @param width ширина полосы, в пикселях
 * @param padding внутреннее поле полосы с каждой стороны, в пикселях
 * @param frameWidth ширина рамки, долей полосы
 * @return доля хода камеры, от нуля до единицы
 */
internal fun scrubbedPositionOf(
  x: Float,
  width: Float,
  padding: Float,
  frameWidth: Float
): Float {
  val track = width - padding * 2f
  val room = 1f - frameWidth
  if (track <= 0f || room <= 0f) {
    return 0f
  }
  val touched = ((x - padding) / track).coerceIn(0f, 1f)
  return ((touched - frameWidth / 2f) / room).coerceIn(0f, 1f)
}

/**
 * Сдвиг по времени, при котором камера окажется в [position] своего хода.
 *
 * Обратная функция к [centreXOf], зажатая диапазоном камеры. Зажатие здесь не страховка, а само
 * определение: где камере можно быть, отвечает `cameraRangeOf`, и второго ответа на этот вопрос в
 * коде быть не должно.
 *
 * Деления нет по построению, поэтому вырожденная история отдельного случая не требует: у пустого
 * графа диапазон камеры вырожден в ноль, и функция вернёт ноль.
 *
 * @param position доля хода камеры по времени
 * @param scale масштаб содержимого
 * @param centreSpan отрезок центров крайних плашек в координатах полотна
 * @param viewport размер видимой области
 * @param range где камере разрешено быть при этом масштабе
 * @return сдвиг по оси времени в пикселях экрана
 */
internal fun scrubbedCameraXOf(
  position: Float,
  scale: Float,
  centreSpan: ClosedFloatingPointRange<Float>,
  viewport: IntSize,
  range: GraphCameraRange
): Float {
  val world = centreSpan.endInclusive - centreSpan.start
  val point = centreSpan.start + position.coerceIn(0f, 1f) * world
  return (viewport.width / 2f - point * scale).coerceIn(range.x)
}

/**
 * Засечки веток: где по времени начинается каждая ветка, кроме магистрали.
 *
 * Группировка идёт по **ветке**, а не по дорожке, и это не педантизм: дорожка освобождается после
 * слияния и переиспользуется (§4.2 брифа), поэтому две несвязанные темы, вставшие на неё по
 * очереди, дали бы **одну** засечку на позиции первой — и вторая тема исчезла бы с мини-карты, не
 * уронив ни теста, ни компилятора.
 *
 * Начало ветки — **минимальная** координата её узлов, а не первый её узел в списке: список
 * упорядочен временем, а не ветками, и опора на совпадение этих порядков — ровно тот вид
 * зависимости, которого раскладка избегает в `centreSpanX`.
 *
 * Результат упорядочен по позиции, потому что порядок обхода `HashMap` не определён, а список,
 * меняющий порядок при том же содержимом, заставлял бы `derivedStateOf` считать себя изменившимся
 * на каждой раскладке.
 *
 * @param branchIds ветка каждого узла
 * @param lanes номер дорожки каждого узла: задаёт направление засечки
 * @param nodeColors цвет идентичности каждого узла
 * @param centres центры плашек в координатах полотна, в порядке [branchIds]
 * @param centreSpan отрезок центров крайних плашек
 * @return засечки в порядке возрастания позиции; пусто, когда веток нет или история вырождена
 */
internal fun laneMarksOf(
  branchIds: List<Branch.Id>,
  lanes: List<Int>,
  nodeColors: List<Color>,
  centres: List<Offset>,
  centreSpan: ClosedFloatingPointRange<Float>
): List<GraphLaneMark> {
  val world = centreSpan.endInclusive - centreSpan.start
  if (world <= 0f || branchIds.isEmpty()) {
    return emptyList()
  }
  val startByBranch = HashMap<Branch.Id, GraphLaneMark>()
  branchIds.forEachIndexed { index, branchId ->
    if (lanes[index] != 0) {
      val x = centres[index].x
      val position = ((x - centreSpan.start) / world).coerceIn(0f, 1f)
      val known = startByBranch[branchId]
      if (known == null || position < known.position) {
        startByBranch[branchId] = GraphLaneMark(
          position = position,
          lane = lanes[index],
          color = nodeColors[index]
        )
      }
    }
  }
  return startByBranch.values.sortedBy { it.position }
}
