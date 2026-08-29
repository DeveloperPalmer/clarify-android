package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColor
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Точка ветвления — место на магистрали, откуда уходит ветка (§6.4 брифа).
 *
 * Стоит на **всех уровнях детализации**: §5 брифа числит ветвление в скелете смысла, который не
 * исчезает ни при каком упрощении. Уровень записан здесь, а не в имени, — иначе перевод узла между
 * уровнями стал бы переименованием.
 *
 * Заливка непрозрачная и берёт цвет полотна: узел стоит **на** линии магистрали и обязан её
 * прорезать, а не пропускать сквозь себя, — та же причина, по которой залит [GlyphNode].
 *
 * Указателя направления у узла нет вовсе, и заводить его заново незачем: время на этом графе идёт
 * слева направо всегда, а куда ушла ветка — вверх или вниз — показывает её собственное ребро. Шеврон
 * стоял здесь в 26 dp от центра круга — ровно столько же между дорожками на обзоре, — и ложился на
 * соседнюю дорожку; вдобавок он был нейтрально-серым и читался отдельным маркером, а не частью
 * линии, которая и так всё сказала.
 *
 * Узел декоративен: §6.4 не даёт ему ни состояний, ни интерактива, а структуру скринридер читает из
 * подписи узла ветки.
 *
 * @param contentDescription связная подпись для скринридера (§14). Тапа у узла нет, а подпись есть:
 *   рёбра скринридер не читает вовсе, и о том, что здесь начинается ветка, сказать больше некому
 * @param laneColor цвет идентичности ветки, которая здесь начинается: обводка и иконка (§7).
 *   Приходит готовым цветом, потому что знать свою дорожку узлу неоткуда
 * @param modifier модификатор узла
 */
@Composable
internal fun ForkNode(
  laneColor: Color,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(AppTheme.colors.backgroundPrimary, CircleShape)
        .border(2.dp, laneColor, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        modifier = Modifier.size(14.dp),
        painter = painterResource(R.drawable.ic_git_fork_24),
        tint = laneColor,
        contentDescription = null
      )
    }
  }
}

@Preview
@Composable
private fun ForkNodePreviewLight(
  @PreviewParameter(ForkNodePreviewProvider::class)
  fork: ForkNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    ForkNodePreviewContent(fork)
  }
}

@Preview
@Composable
private fun ForkNodePreviewDark(
  @PreviewParameter(ForkNodePreviewProvider::class)
  fork: ForkNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    ForkNodePreviewContent(fork)
  }
}

@Composable
private fun ForkNodePreviewContent(fork: ForkNodePreview) {
  ForkNode(
    // Через настоящий маппер, а не через свой цвет: кадр заодно проверяет, что соседние дорожки
    // получают разные оттенки.
    laneColor = fork.lane.toBranchColor(AppTheme.colors),
    contentDescription = "Ответвление"
  )
}

@Immutable
private data class ForkNodePreview(val lane: Int)

/**
 * Кадры превью [ForkNode]: разные дорожки.
 *
 * Направления в кадрах больше нет — его показывает ребро ухода, которого в превью узла не
 * существует. Оттенков три, потому что единственное, что здесь проверяется, — что цвет приходит
 * параметром и берётся из палитры идентичности.
 */
@Immutable
private class ForkNodePreviewProvider : PreviewParameterProvider<ForkNodePreview> {
  override val values = sequenceOf(
    ForkNodePreview(lane = 1),
    ForkNodePreview(lane = 2),
    ForkNodePreview(lane = 5)
  )
}
