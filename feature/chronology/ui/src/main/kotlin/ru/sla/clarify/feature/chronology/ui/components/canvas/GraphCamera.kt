package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphPanStep
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement

/*
 * Камера полотна: где ей разрешено быть, где она стоит в покое, как на неё ложится дельта и когда
 * инерцию пора обрывать.
 *
 * Отделено от раскладки намеренно. Раскладка отвечает, где стоят плашки, и о камере не знает
 * вовсе — а здесь ни одна функция не знает, как получились координаты плашек. Пока обе темы
 * лежали в одном файле, правка любой из них заставляла читать чужую.
 */

/**
 * Допустимый сдвиг содержимого по оси времени.
 *
 * Камере разрешено наводиться на любой центр плашки и только на него: в покое в центре экрана стоит
 * самый левый узел, а докрутив вправо до упора — самый правый. Кламп по краям содержимого давал бы
 * обратное: начало истории у левой кромки, конец у правой.
 *
 * @param centreSpanX отрезок центров плашек в координатах полотна
 * @param viewport ширина видимой области
 * @return диапазон сдвига, вырожденный в точку при единственном узле
 */
internal fun timelinePanRangeOf(
  centreSpanX: ClosedFloatingPointRange<Float>,
  viewport: Float
): ClosedFloatingPointRange<Float> {
  val centre = viewport / 2f
  return (centre - centreSpanX.endInclusive)..(centre - centreSpanX.start)
}

/**
 * Допустимый сдвиг содержимого по одной оси.
 *
 * Камера — это сдвиг содержимого: `экран = полотно + камера`. Чтобы начало содержимого встало у
 * начала экрана, нужен сдвиг `-min`; чтобы конец встал у конца экрана — `viewport - max`.
 *
 * Когда содержимое короче экрана, границы схлопываются в одно центрирующее значение: прижимать к
 * краю то, что помещается целиком, незачем. Когда содержимого нет вовсе, центрировать нечего и
 * сдвиг остаётся нулевым — иначе пустое полотно уезжало бы на пол-экрана.
 *
 * @param min начало содержимого в координатах полотна
 * @param max конец содержимого в координатах полотна
 * @param viewport размер видимой области по той же оси
 * @return диапазон сдвига, вырожденный в точку, когда содержимое помещается целиком
 */
internal fun panRangeOf(min: Float, max: Float, viewport: Float): ClosedFloatingPointRange<Float> {
  if (max <= min) {
    return 0f..0f
  }
  val lower = viewport - max
  // Именно `0f - min`, а не `-min`: у нуля унарный минус даёт отрицательный нуль, и граница
  // печаталась как «-0.0».
  val upper = 0f - min
  if (lower <= upper) {
    return lower..upper
  }
  val centered = (viewport - (max - min)) / 2f - min
  return centered..centered
}

/**
 * Где камере разрешено быть при этой раскладке и этом вьюпорте.
 *
 * Собирает обе оси в одну величину, чтобы «где камере можно быть» имело единственное определение:
 * то же самое значение читается при показе, проверяется при записи и пере-накладывается после
 * раскладки. Пока определение жило выражением внутри чтения, запись о нём не знала.
 *
 * @param placement последняя раскладка графа
 * @param viewport размер видимой области
 * @return диапазоны по обеим осям; вырожденные — норма, а не краевой случай
 */
internal fun cameraRangeOf(placement: GraphPlacement, viewport: IntSize): GraphCameraRange {
  if (placement.isEmpty) {
    return GraphCameraRange.Empty
  }
  return GraphCameraRange(
    x = timelinePanRangeOf(placement.centreSpanX, viewport.width.toFloat()),
    y = panRangeOf(placement.bounds.top, placement.bounds.bottom, viewport.height.toFloat())
  )
}

/**
 * Где камера стоит, пока её не двигали.
 *
 * Наводится на **первую** плашку, а не прижимается к краю содержимого: экран открывают, чтобы
 * увидеть начало истории, и оно должно оказаться под глазами, а не в углу. Наведение зажимается
 * диапазоном — у короткой истории центр экрана недостижим, и тогда камера встаёт настолько близко
 * к нему, насколько содержимое позволяет.
 *
 * Который из центров первый, решается здесь, а не в раскладке: раскладка отвечает, где стоят
 * плашки, и знать о том, что одна из них — начало истории, ей незачем.
 *
 * @param placement последняя раскладка графа
 * @param viewport размер видимой области
 * @param range где камере разрешено быть
 * @return положение камеры в покое
 */
internal fun cameraRestOf(
  placement: GraphPlacement,
  viewport: IntSize,
  range: GraphCameraRange
): Offset {
  if (placement.isEmpty) {
    return Offset.Zero
  }
  val first = placement.centres.first()
  val centred = Offset(
    x = viewport.width / 2f - first.x,
    y = viewport.height / 2f - first.y
  )
  return range.clamp(centred)
}

/**
 * Двигает камеру на [delta], не выпуская её за [range].
 *
 * Кламп стоит на записи, а не на чтении, и это не перестановка мест. Накапливая незажатый сдвиг,
 * состояние банкует перерегулирование: упор в стенку на три тысячи пикселей превращается в мёртвую
 * зону такой же величины, которая сама не рассасывается — жест обратно сначала выбирает её и только
 * потом двигает картинку. Вылезает это не залипанием, а телепортом: следующая раскладка расширяет
 * диапазон, и камера за кадр уезжает на величину банка.
 *
 * @param camera текущий сдвиг содержимого
 * @param delta запрошенное приращение
 * @param range где камере разрешено быть
 * @return новый сдвиг и та часть [delta], которая в него уместилась
 */
internal fun panStepOf(camera: Offset, delta: Offset, range: GraphCameraRange): GraphPanStep {
  val moved = Offset(
    x = (camera.x + delta.x).coerceIn(range.x),
    y = (camera.y + delta.y).coerceIn(range.y)
  )
  return GraphPanStep(
    requested = delta,
    camera = moved,
    consumed = moved - camera
  )
}

/**
 * Упёрлась ли камера по всем осям, которые несут бросок.
 *
 * Признак смотрит на диапазон и на направление, а не на то, сколько взяла последняя дельта, и это
 * важнее, чем кажется. Первый кадр затухания приходит с нулевым приращением, и правило вида
 * «потребили меньше запрошенного» остановило бы бросок, не начав его. Оно же не умеет отличить
 * упор от оси, которая в броске просто не участвует.
 *
 * Условие `&&` — осознанный отход от платформы. Платформа гасит диагональ целиком, стоит упереться
 * одной оси; здесь ось Y вырождена, пока дорожка одна, поэтому любой слегка наклонный бросок умирал
 * бы мгновенно, и «fling на 360» работал бы ровно для одного угла. Пока бросок несёт хоть одна ось,
 * инерция живёт и едет вдоль стенки — как список, а не как стена.
 *
 * Цена решения: на вырожденной оси бросок проезжает только свою проекцию, но тратит на неё полную
 * длительность. Для пологих бросков это незаметно, для крутых — 80° дают 112 dp за те же 924 мс.
 * Если на устройстве это прочтётся как заедание, обратный ход — заменить `&&` на `||`.
 *
 * @param camera текущий сдвиг содержимого
 * @param direction единичный вектор броска, см. [FlingDirection]
 * @param range где камере разрешено быть
 * @return `true`, когда двигаться некуда и затухание пора обрывать
 */
internal fun isCameraStuck(camera: Offset, direction: Offset, range: GraphCameraRange): Boolean {
  return isAxisStuck(
    camera = camera.x,
    direction = direction.x,
    range = range.x
  ) && isAxisStuck(
    camera = camera.y,
    direction = direction.y,
    range = range.y
  )
}

/**
 * Упёрлась ли одна ось.
 *
 * Нулевая компонента направления и вырожденный диапазон — одно и то же: нести бросок этой оси нечем.
 *
 * @param camera сдвиг по этой оси
 * @param direction компонента направления броска
 * @param range допустимый сдвиг по этой оси
 * @return `true`, когда по этой оси ехать некуда
 */
private fun isAxisStuck(
  camera: Float,
  direction: Float,
  range: ClosedFloatingPointRange<Float>
): Boolean {
  if (direction == 0f || range.start >= range.endInclusive) {
    return true
  }
  return if (direction > 0f) {
    camera >= range.endInclusive
  } else {
    camera <= range.start
  }
}
