package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphDebugInfo
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Отладочная панель камеры полотна, под тоглом `chronologyDebugOverlay`.
 *
 * Панель отвечает на два вопроса, которые не разделить по внешнему виду графа: доходит ли жест до
 * обработчика (`drags`) и не упёрлась ли камера в границу (`camera` против `rangeX`/`rangeY`).
 * Пока панорамирование не устоялось, снимать её рано.
 */
@Composable
internal fun GraphDebugOverlay(
  info: GraphDebugInfo,
  modifier: Modifier = Modifier
) {
  BasicText(
    modifier = modifier
      .background(AppTheme.colors.backgroundTertiary)
      .padding(6.dp),
    text = buildString {
      appendLine("viewport = ${info.viewportWidth} x ${info.viewportHeight}")
      appendLine("declared = ${info.declaredBounds}")
      appendLine("bounds   = ${info.contentBounds}")
      appendLine("rangeX   = ${info.cameraMinX} .. ${info.cameraMaxX}")
      appendLine("rangeY   = ${info.cameraMinY} .. ${info.cameraMaxY}")
      appendLine("camera   = ${info.camera}${if (info.isCameraMoved) "" else " (at rest)"}")
      append("drags    = ${info.dragCount}, last = ${info.lastDrag}")
    },
    style = AppTheme.typography.caption.copy(color = AppTheme.colors.contentPrimary)
  )
}
