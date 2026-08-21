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
 * Отладочная панель полотна, под тоглом `chronologyDebugOverlay`.
 *
 * Панель отвечает на два вопроса, которые не разделить по внешнему виду графа: доходит ли жест до
 * обработчика (`drags`) и не упёрлась ли камера в границу (`camera` против `bounds`). Пока
 * панорамирование не устоялось, снимать её рано.
 *
 * Это единственное место, где значения камеры читаются в композиции, и потому единственное, что
 * рекомпонуется на кадрах панорамирования. Цена включённого тогла, не более.
 *
 * @param info снимок камеры и последней раскладки
 * @param modifier модификатор панели
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
      appendLine("bounds   = ${info.contentBounds}")
      appendLine("camera   = ${info.camera}${if (info.isCameraMoved) "" else " (at rest)"}")
      appendLine("nodes    = ${info.nodeCount}, edges = ${info.edgeCount}")
      appendLine("layouts  = ${info.layoutRevision}")
      append("drags    = ${info.dragCount}, last = ${info.lastDrag}")
    },
    style = AppTheme.typography.caption.copy(color = AppTheme.colors.contentPrimary)
  )
}
