package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Счётчики проходов Compose по полотну.
 *
 * Одна и та же форма используется и для накопленных значений, и для скоростей в секунду: зависание
 * видно не по абсолютному числу, а по тому, что какая-то фаза крутится, пока пользователь ничего не
 * делает.
 *
 * @param canvasCompositions рекомпозиции тела полотна
 * @param nodeCompositions рекомпозиции содержимого узлов, суммарно по всем узлам
 * @param overlayCompositions рекомпозиции самой отладочной панели
 * @param measurePasses проходы измерения слоя узлов
 * @param placementPasses проходы размещения узлов
 * @param layerUpdates пересчёты свойств слоя камеры
 * @param edgeDraws перерисовки связей
 * @param backdropDraws перерисовки фона
 * @param panEvents события жеста, дошедшие до камеры
 * @param flingSteps кадры затухания, дошедшие до камеры
 * @param flingStalls кадры затухания, на которых камера не взяла ничего
 */
@Immutable
data class GraphTelemetry(
  val canvasCompositions: Int,
  val nodeCompositions: Int,
  val overlayCompositions: Int,
  val measurePasses: Int,
  val placementPasses: Int,
  val layerUpdates: Int,
  val edgeDraws: Int,
  val backdropDraws: Int,
  val panEvents: Int,
  val flingSteps: Int,
  val flingStalls: Int
) {

  companion object {
    val Empty = GraphTelemetry(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
  }
}
