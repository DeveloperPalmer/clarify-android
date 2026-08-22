package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugRow
import ru.sla.clarify.feature.chronology.ui.entity.GraphTelemetry

/**
 * Счётчики фаз Compose в строки отладочной панели.
 *
 * Пределы разные, потому что фазы разные по назначению: измерение, размещение и рекомпозиция полотна
 * в покое обязаны стоять, а слой камеры, связи, фон, панель и жест идут покадрово.
 *
 * @param rates те же счётчики в единицах в секунду
 * @return строки левой колонки панели
 */
internal fun GraphTelemetry.toPhaseRows(rates: GraphTelemetry): List<GraphDebugRow> {
  return listOf(
    phaseRow("canvas", canvasCompositions, rates.canvasCompositions, IDLE_LIMIT),
    phaseRow("nodes", nodeCompositions, rates.nodeCompositions, NODE_LIMIT),
    phaseRow("overlay", overlayCompositions, rates.overlayCompositions, FRAME_LIMIT),
    phaseRow("measure", measurePasses, rates.measurePasses, IDLE_LIMIT),
    phaseRow("placement", placementPasses, rates.placementPasses, IDLE_LIMIT),
    phaseRow("layer", layerUpdates, rates.layerUpdates, FRAME_LIMIT),
    phaseRow("edges", edgeDraws, rates.edgeDraws, FRAME_LIMIT),
    phaseRow("backdrop", backdropDraws, rates.backdropDraws, FRAME_LIMIT),
    phaseRow("pan", panEvents, rates.panEvents, FRAME_LIMIT)
  )
}

/**
 * Строка фазы: скорость впереди, накопленное значение следом.
 *
 * Скорость стоит первой не для красоты: зависание видно по тому, что фаза тикает при снятом пальце,
 * а накопленное число за смену только растёт и само по себе ни о чём не говорит.
 *
 * @param label подпись фазы
 * @param total накопленное значение
 * @param rate значений в секунду
 * @param limit предел, выше которого значение аномально
 * @return строка панели
 */
private fun phaseRow(label: String, total: Int, rate: Int, limit: Int): GraphDebugRow {
  return GraphDebugRow(
    label = label,
    value = "$rate/s · $total",
    isAnomalous = rate > limit
  )
}

// Фазы, которые в покое обязаны стоять. Несколько проходов на одно действие пользователя — норма,
// устойчивый поток — уже нет: именно так выглядит цикл «запись из измерения, чтение в композиции».
private const val IDLE_LIMIT = 8

// Рекомпозиция содержимого считается суммой по всем узлам, поэтому один полный проход небольшого
// графа сам по себе даёт десятки.
private const val NODE_LIMIT = 60

// Фазы, которые по построению идут покадрово: слой камеры, связи, фон, панель и сам жест. Выше
// частоты обновления экрана это означает больше одного прохода на кадр.
private const val FRAME_LIMIT = 140
