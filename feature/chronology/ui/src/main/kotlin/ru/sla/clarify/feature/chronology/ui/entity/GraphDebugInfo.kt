package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Снимок состояния камеры полотна для отладочной панели.
 *
 * Величины в пикселях — ровно в тех, в которых считаются границы и сдвиг, чтобы читать панель
 * можно было без пересчёта в dp.
 */
@Immutable
data class GraphDebugInfo(
  val viewportWidth: Int,
  val viewportHeight: Int,
  val declaredBounds: Rect,
  val contentBounds: Rect,
  val cameraMinX: Float,
  val cameraMaxX: Float,
  val cameraMinY: Float,
  val cameraMaxY: Float,
  val camera: Offset,
  val isCameraMoved: Boolean,
  val dragCount: Int,
  val lastDrag: Offset
)
