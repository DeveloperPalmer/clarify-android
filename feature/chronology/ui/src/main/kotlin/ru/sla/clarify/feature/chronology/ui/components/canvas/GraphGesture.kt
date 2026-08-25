package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * Жест камеры: протяжка одним пальцем, пинч двумя и скорость отпускания под инерцию.
 *
 * Собран руками из публичных вычислений foundation, и это вынужденно, а не из любви к своему коду.
 * Готовые детекторы дают либо одно, либо другое: `detectDragGestures` знает момент отпускания и
 * отдаёт `PointerInputChange`, но ведёт жест по одному указателю, а `detectTransformGestures` и
 * `Modifier.transformable` считают пинч, но об отпускании не сообщают вовсе — у `TransformStopped`
 * нет ни скорости, ни позиции. Инерция на этом экране снимается в момент отпускания, поэтому взять
 * пинч из коробки и сохранить бросок нельзя. Сами вычисления при этом берутся у платформы целиком:
 * [calculateZoom], [calculatePan], [calculateCentroid], [calculateCentroidSize].
 *
 * Порог схватывания повторяет платформенный дословно: `|1 − zoom| · centroidSize > touchSlop` или
 * `|pan| > touchSlop`. Своя формула означала бы, что пинч на этом экране берётся не так, как во всех
 * остальных приложениях устройства.
 *
 * Скорость снимается по **центроиду**, а не по указателю: при пинче именно центроид и есть движение
 * картинки. Отсюда же и сброс трекера при смене числа пальцев — см. тело цикла.
 *
 * @param isBlocked лежит ли точка касания на том, что закрывает полотно: жест, начатый внутри
 *   инструмента, до камеры не доходит вовсе
 * @param onTouch касание, отбирающее камеру у инерции; зовётся до порога схватывания, потому что
 *   палец обязан останавливать бросок сразу, а не после первого движения
 * @param onTransform шаг жеста: точка, которую жест держит на месте, сдвиг центроида и множитель
 *   масштаба
 * @param onRelease скорость в момент отпускания, в пикселях в секунду
 */
internal suspend fun PointerInputScope.detectCameraGestures(
  isBlocked: (Offset) -> Boolean,
  onTouch: () -> Unit,
  onTransform: (focus: Offset, pan: Offset, zoom: Float) -> Unit,
  onRelease: (Velocity) -> Unit
) {
  val tracker = VelocityTracker()
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    // Решение принимается один раз, на касании, и держится весь жест: палец, ушедший с панели на
    // полотно, не должен посреди движения начать таскать камеру. Выйти отсюда безопасно —
    // awaitEachGesture сам дождётся, пока все пальцы уйдут.
    if (isBlocked(down.position)) {
      return@awaitEachGesture
    }
    onTouch()
    tracker.resetTracking()
    // Через тот же путь, что и остальные точки: на событии нажатия трекер заводит свой накопитель
    // позиций, и подсунутая мимо него точка сместила бы всю дальнейшую траекторию.
    tracker.addPointerInputChange(down)

    var pointers = 1
    var pastSlop = false
    var zoom = 1f
    var pan = Offset.Zero
    val slop = viewConfiguration.touchSlop
    var event: PointerEvent
    do {
      event = awaitPointerEvent()
      // Жест перехвачен кем-то ещё: накопленная скорость к нему больше не относится.
      if (event.changes.fastAny { it.isConsumed }) {
        tracker.resetTracking()
        return@awaitEachGesture
      }
      val pressed = event.changes.count { it.pressed }
      val zoomChange = event.calculateZoom()
      val panChange = event.calculatePan()
      val centroid = event.calculateCentroid(useCurrent = false)
      trackVelocity(tracker, event, centroid, pressed, pointers)
      pointers = pressed
      if (!pastSlop) {
        zoom *= zoomChange
        pan += panChange
        val centroidSize = event.calculateCentroidSize(useCurrent = false)
        pastSlop = abs(1f - zoom) * centroidSize > slop || pan.getDistance() > slop
      }
      if (pastSlop) {
        if (centroid.isSpecified && (zoomChange != 1f || panChange != Offset.Zero)) {
          onTransform(centroid, panChange, zoomChange)
        }
        event.changes.fastForEach {
          if (it.positionChanged()) {
            it.consume()
          }
        }
      }
    } while (event.changes.fastAny { it.pressed })

    if (pastSlop) {
      // Кламп по осям, как у платформы: у трекера полиномиальная подгонка, и дрожание перед
      // отпусканием умеет отдать десятки тысяч px/s, а пролёт растёт как v^1.736.
      val maximum = viewConfiguration.maximumFlingVelocity
      onRelease(tracker.calculateVelocity(Velocity(maximum, maximum)))
    }
  }
}

/**
 * Кормит трекер скорости тем, что в этот момент и есть движение картинки.
 *
 * Один палец идёт платформенным путём — [addPointerInputChange] с историческими точками внутри
 * события и с точкой отпускания. Это не украшение: `VelocityTracker` считает по методу наименьших
 * квадратов второй степени и **возвращает ноль**, если точек меньше трёх или между соседними
 * прошло больше сорока миллисекунд. Короткий флик даёт три-четыре события, поэтому потеря истории
 * и финальной точки обнуляет бросок целиком, а не портит его слегка.
 *
 * Два пальца и больше идут центроидом: при пинче пальцы разъезжаются в разные стороны, и скорость
 * любого из них — не скорость картинки. Смена состава сбрасывает накопленное: центроид в этот момент
 * скачком переезжает на половину расстояния между пальцами, и трекер прочитал бы это как рывок в
 * тысячи пикселей в секунду, которого рукой не делали.
 *
 * @param tracker трекер жеста
 * @param event событие указателей
 * @param centroid центроид предыдущих позиций, [Offset.Unspecified] при отпускании последнего пальца
 * @param pressed сколько указателей нажато сейчас
 * @param previous сколько было нажато на прошлом событии
 */
private fun trackVelocity(
  tracker: VelocityTracker,
  event: PointerEvent,
  centroid: Offset,
  pressed: Int,
  previous: Int
) {
  if (pressed > 1) {
    if (previous <= 1) {
      tracker.resetTracking()
    }
    if (centroid.isSpecified) {
      tracker.addPosition(event.changes.first().uptimeMillis, centroid)
    }
    return
  }
  if (previous > 1) {
    tracker.resetTracking()
  }
  // Нажатый указатель, а при отпускании последнего — он же сам: его up-событие и есть та финальная
  // точка, без которой скорость считается по данным двухкадровой давности.
  val change = event.changes.fastFirstOrNull { it.pressed } ?: event.changes.first()
  tracker.addPointerInputChange(change)
}
