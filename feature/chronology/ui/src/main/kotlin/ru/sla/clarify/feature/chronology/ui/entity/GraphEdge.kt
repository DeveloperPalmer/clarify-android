package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Связь двух соседних узлов одной дорожки: родитель — ребёнок.
 *
 * Отрезок задан в координатах полотна, в пикселях, и живёт строго в зазоре между плашками: от
 * правого края предыдущего узла до левого края следующего. Под плашку линия не заходит — узел
 * бывает полупрозрачным (отправляющееся сообщение), и линия просвечивала бы через него.
 */
@Immutable
data class GraphEdge(
  val startX: Float,
  val endX: Float,
  val y: Float
)
