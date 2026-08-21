package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Снимок состояния полотна для отладочной панели.
 *
 * Величины в пикселях — ровно в тех, в которых считаются границы и сдвиг, чтобы панель читалась без
 * пересчёта в dp.
 *
 * @param viewportWidth ширина видимой области
 * @param viewportHeight высота видимой области
 * @param contentBounds границы содержимого в координатах полотна
 * @param camera сдвиг содержимого после клампа
 * @param isCameraMoved двигали ли камеру хоть раз
 * @param nodeCount число узлов в модели
 * @param edgeCount число выведенных связей
 * @param dragCount число событий жеста, дошедших до обработчика
 * @param lastDrag последнее приращение жеста
 */
@Immutable
data class GraphDebugInfo(
  val viewportWidth: Int,
  val viewportHeight: Int,
  val contentBounds: Rect,
  val camera: Offset,
  val isCameraMoved: Boolean,
  val nodeCount: Int,
  val edgeCount: Int,
  val dragCount: Int,
  val lastDrag: Offset
)
