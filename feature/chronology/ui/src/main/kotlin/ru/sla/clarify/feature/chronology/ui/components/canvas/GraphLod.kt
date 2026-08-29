package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevelBand

/*
 * Уровни детализации: где каждый живёт по масштабу, когда полотно уходит на соседний и с каким
 * масштабом там приземляется.
 *
 * Отделено от камеры так же, как камера отделена от раскладки: здесь знают, что у уровня есть полоса
 * масштаба, и не знают ни как получились координаты плашек, ни как зажимается сдвиг. Обратной
 * зависимости нет — камера про уровни не знает вовсе.
 *
 * Величина, которая здесь считается, — не масштаб, а **охват**: сколько истории видно на экране.
 * Масштаб между уровнями несравним, охват сравним, и сохраняется при переходе именно он. Наивная
 * схема «порог на сыром масштабе, масштаб сохраняется» даёт на стыке скачок в 6.2 раза: тот же 0.4×
 * на сжатой вчетверо раскладке показывает не четыре узла, а двадцать семь.
 */

/**
 * Полоса масштаба уровня.
 *
 * @param level уровень детализации
 * @param fitScale масштаб, при котором содержимое видно целиком, см. [fitScaleOf]
 * @return пределы, за которыми уровень сменяется соседним
 */
internal fun graphLevelBandOf(level: GraphLevel, fitScale: Float): GraphLevelBand {
  return when (level) {
    // Диапазон §11.1 брифа, зафиксированный владельцем; уход в обзор его не сужает.
    GraphLevel.Episodes -> GraphLevelBand(min = 0.4f, max = 2.5f)
    // Нижний край обзора — «видно всё», но не глубже 0.2×. На демо-наборе это 0.305×, и упор
    // приходится ровно на вписанный граф; на переписке в двести эпизодов вписывание потребовало бы
    // 0.077×, где глиф 14 dp вырождается в полтора пикселя и обзор перестаёт быть картой.
    GraphLevel.Overview -> GraphLevelBand(min = fitScale.coerceIn(0.2f, 0.4f), max = 2.5f)
  }
}

/**
 * Масштаб, при котором содержимое видно целиком.
 *
 * Берётся более тесная из двух осей: вписать по времени, вылезая за экран по дорожкам, значит не
 * вписать вовсе. Поля полотна входят в [bounds] и вписываются вместе с ним — они и есть та рамка,
 * без которой крайняя плашка упирается в кромку экрана.
 *
 * @param bounds границы содержимого в координатах полотна
 * @param viewport размер видимой области
 * @return масштаб; единица у вырожденного содержимого — вписывать нечего
 */
internal fun fitScaleOf(bounds: Rect, viewport: IntSize): Float {
  if (bounds.width <= 0f || bounds.height <= 0f || viewport.width <= 0 || viewport.height <= 0) {
    return 1f
  }
  return minOf(viewport.width / bounds.width, viewport.height / bounds.height)
}

/**
 * Уровень, на который уводит запрошенный жестом масштаб.
 *
 * Смотрит на **запрошенный** масштаб, а не на принятый: принятый уже зажат полосой уровня и о выходе
 * за неё ничего не скажет.
 *
 * @param level уровень, на котором полотно стоит сейчас
 * @param requestedScale масштаб, которого просит жест
 * @param band полоса текущего уровня
 * @return соседний уровень или `null`, если остаёмся на этом
 */
internal fun graphLevelSwitchOf(
  level: GraphLevel,
  requestedScale: Float,
  band: GraphLevelBand
): GraphLevel? {
  // Не-конечный множитель приходит из арифметики жеста, а не из UX: на вырожденном событии частное
  // центроидов обращается в NaN, и сравнение с ним молча даёт `false` в обе стороны.
  if (!requestedScale.isFinite()) {
    return null
  }
  return when {
    level == GraphLevel.Episodes && requestedScale < band.min -> GraphLevel.Overview
    level == GraphLevel.Overview && requestedScale > band.max -> GraphLevel.Episodes
    else -> null
  }
}

/**
 * Масштаб посадки на новом уровне: тот, при котором на экране остаётся тот же охват.
 *
 * `scale · centreSpan` — это сколько пикселей экрана занимает вся история, поэтому равенство этого
 * произведения до и после перехода и означает «видно то же самое». На демо-наборе отношение охватов
 * равно 6.23 при отношении пределов масштаба 6.25 — отсюда и берётся почти бесшовный стык.
 *
 * Посадка держится в [margin] от краёв полосы, и это весь гистерезис §5: приземлившись ровно на
 * порог, с которого только что ушли, полотно мигало бы уровнем от дрожания пальца.
 *
 * @param scaleBefore масштаб на уходящем уровне
 * @param spanBefore охват уходящего уровня
 * @param spanAfter охват нового уровня
 * @param band полоса нового уровня
 * @param margin запас от краёв полосы, долей
 * @return масштаб посадки внутри полосы
 */
internal fun levelLandingOf(
  scaleBefore: Float,
  spanBefore: Float,
  spanAfter: Float,
  band: GraphLevelBand,
  margin: Float
): Float {
  val lower = band.min * (1f + margin)
  val upper = band.max * (1f - margin)
  // Полоса уже двух запасов: держаться от обоих краёв разом невозможно, и середина — единственное
  // место, равноудалённое от них.
  if (upper <= lower) {
    return (band.min + band.max) / 2f
  }
  // Вырожденная история: охват мерить нечем, и сохранять нечего. Дальний край полосы — то, куда
  // ведёт переход по смыслу: ушли вниз — приземлились наверху соседней полосы.
  if (spanBefore <= 0f || spanAfter <= 0f || !scaleBefore.isFinite()) {
    return upper
  }
  return (scaleBefore * spanBefore / spanAfter).coerceIn(lower, upper)
}

/**
 * Встречный масштаб уходящего представления на время кроссфейда.
 *
 * Слой камеры к этому моменту уже приземлился на новый масштаб, и плашка, нарисованная внутри него
 * как есть, растянулась бы в `to / from` раз. Встречный множитель оставляет ей ровно тот размер,
 * каким она была на экране в момент перехода, — гаснет она на месте, а не разрастаясь.
 *
 * @param from масштаб, на котором полотно ушло с прошлого уровня
 * @param to масштаб прямо сейчас
 * @return множитель для уходящего представления
 */
internal fun counterScaleOf(from: Float, to: Float): Float {
  if (to <= 0f || !from.isFinite() || !to.isFinite()) {
    return 1f
  }
  return from / to
}
