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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.ViewConfiguration
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
 * Скорость под инерцию снимается **только с протяжки одним пальцем**: жест, в котором побывал второй
 * палец, отпускается без броска — см. тело цикла.
 *
 * @param isBlocked лежит ли точка касания на том, что закрывает полотно: жест, начатый внутри
 *   инструмента, до камеры не доходит вовсе
 * @param onTouch касание, отбирающее камеру у инерции; зовётся до порога схватывания, потому что
 *   палец обязан останавливать бросок сразу, а не после первого движения
 * @param onTransform шаг жеста: точка, которую жест держит на месте, сдвиг центроида и множитель
 *   масштаба
 * @param onRelease скорость в момент отпускания, в пикселях в секунду
 * @param onDoubleTap два тапа подряд по фону: «вписать всё» и возврат (§11.1 брифа)
 */
internal suspend fun PointerInputScope.detectCameraGestures(
  isBlocked: (Offset) -> Boolean,
  onTouch: () -> Unit,
  onTransform: (focus: Offset, pan: Offset, zoom: Float) -> Unit,
  onRelease: (Velocity) -> Unit,
  onDoubleTap: () -> Unit
) {
  val tracker = VelocityTracker()
  // Отпускание предыдущего тапа: из него и следующего касания и складывается двойной тап. Живёт
  // между итерациями, потому что тапов два, а жест каждый раз новый. Пороги платформенные — свои
  // означали бы, что двойной тап здесь берётся не так, как во всех остальных приложениях устройства.
  var previousUp: PointerInputChange? = null
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    // Решение принимается один раз, на касании, и держится весь жест: палец, ушедший с панели на
    // полотно, не должен посреди движения начать таскать камеру. Выйти отсюда безопасно —
    // awaitEachGesture сам дождётся, пока все пальцы уйдут.
    if (isBlocked(down.position)) {
      previousUp = null
      return@awaitEachGesture
    }
    val isSecondTap = previousUp?.let { first -> down.followsTap(first, viewConfiguration) } == true
    // Признак снят — само отпускание больше не нужно: третий тап подряд начинает счёт заново, иначе
    // долгая дробь по фону вписывала бы граф через раз.
    previousUp = null
    onTouch()
    tracker.resetTracking()
    // Через тот же путь, что и остальные точки: на событии нажатия трекер заводит свой накопитель
    // позиций, и подсунутая мимо него точка сместила бы всю дальнейшую траекторию.
    tracker.addPointerInputChange(down)

    var pointers = 1
    var isPinch = false
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
      if (pressed > 1 || pointers > 1) {
        // Второй палец на экране — и жест перестал быть протяжкой до самого конца жеста. Трекеру
        // с этого кадра нечего давать: пальцы пинча разъезжаются в разные стороны, и скорость
        // любого из них — не скорость картинки, а центроид, которым картинка и движется, при
        // отрыве первого пальца скачком переезжает на оставшийся. Условие смотрит и на прошлый
        // кадр: пинч схлопывается в один палец, и без памяти о нём кадры схлопывания и отпускания
        // прочитались бы протяжкой.
        isPinch = true
      } else {
        // Платформенный путь — с историческими точками внутри события и с точкой отпускания. Это не
        // украшение: `VelocityTracker` считает методом наименьших квадратов и **возвращает ноль**,
        // если точек меньше трёх или между соседними прошло больше сорока миллисекунд. Короткий
        // флик даёт три-четыре события, поэтому потеря истории обнуляет бросок целиком.
        // Нажатый указатель, а при отпускании последнего — он же сам: его up-событие и есть та
        // финальная точка, без которой скорость считается по данным двухкадровой давности.
        val moved = event.changes.fastFirstOrNull { it.pressed } ?: event.changes.first()
        tracker.addPointerInputChange(moved)
      }
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
      if (!isPinch) {
        // Кламп по осям, как у платформы: у трекера полиномиальная подгонка, и дрожание перед
        // отпусканием умеет отдать десятки тысяч px/s, а пролёт растёт как v^1.736.
        val maximum = viewConfiguration.maximumFlingVelocity
        onRelease(tracker.calculateVelocity(Velocity(maximum, maximum)))
      }
      return@awaitEachGesture
    }
    // Порога жест не прошёл — это тап. Вторым таким же он становится двойным, а первым остаётся
    // ждать следующего касания. Жест, потреблённый узлом, сюда не доходит вовсе, поэтому двойной тап
    // по плашке полотну не достаётся.
    if (isSecondTap) {
      onDoubleTap()
    } else {
      previousUp = event.changes.first()
    }
  }
}

/**
 * Продолжает ли это касание предыдущий тап, то есть складывается ли с ним в двойной.
 *
 * Оба порога платформенные: [ViewConfiguration.doubleTapTimeoutMillis] задаёт, сколько ждать второго
 * касания, [ViewConfiguration.doubleTapMinTimeMillis] отсекает дребезг, а [ViewConfiguration.touchSlop]
 * — расстояние, на котором два тапа ещё считаются одним местом.
 *
 * @param first отпускание предыдущего тапа
 * @param configuration пороги устройства
 * @return `true`, когда касания складываются в двойной тап
 */
private fun PointerInputChange.followsTap(
  first: PointerInputChange,
  configuration: ViewConfiguration
): Boolean {
  val elapsed = uptimeMillis - first.uptimeMillis
  if (elapsed < configuration.doubleTapMinTimeMillis || elapsed > configuration.doubleTapTimeoutMillis) {
    return false
  }
  return (position - first.position).getDistance() <= configuration.touchSlop
}
