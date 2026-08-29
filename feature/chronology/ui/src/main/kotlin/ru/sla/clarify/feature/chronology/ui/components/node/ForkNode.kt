package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.ForkDirection
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
 * Шеврон-указатель в раскладке места не занимает: на дорожку узел ставится центром круга, и коробка,
 * подросшая на шеврон, увела бы центр с линии. Это то же правило, по которому не занимает места гало
 * непрочитанного.
 *
 * Узел декоративен: §6.4 не даёт ему ни состояний, ни интерактива, а структуру скринридер читает из
 * подписи узла ветки.
 *
 * @param contentDescription связная подпись для скринридера (§14). Тапа у узла нет, а подпись есть:
 *   рёбра скринридер не читает вовсе, и о том, что здесь начинается ветка, сказать больше некому
 * @param laneColor цвет идентичности ветки, которая здесь начинается: обводка и иконка (§7).
 *   Приходит готовым цветом, потому что знать свою дорожку узлу неоткуда
 * @param modifier модификатор узла
 * @param direction куда уходит ветка; шеврон повёрнут и переставлен соответственно
 */
@Composable
internal fun ForkNode(
  laneColor: Color,
  contentDescription: String,
  modifier: Modifier = Modifier,
  direction: ForkDirection = ForkDirection.Up
) {
  val up = direction == ForkDirection.Up
  Box(
    modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
    contentAlignment = Alignment.Center
  ) {
    Box(
      // Круг задаёт коробку узла целиком: шеврон ниже смещён `offset`, а он на измерение не влияет.
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
    Icon(
      modifier = Modifier
        .size(12.dp)
        // 26 dp от центра узла: между кругом и шевроном остаётся 2 dp воздуха, и указатель читается
        // как отдельный маркер, а не как часть обводки.
        .offset(y = if (up) (-26).dp else 26.dp)
        // Иконка нарисована смотрящей вниз, поэтому вверх она разворачивается, а не берётся второй.
        .rotate(if (up) 180f else 0f),
      painter = painterResource(R.drawable.ic_chevron_down_24),
      tint = AppTheme.colors.contentQuaternary,
      contentDescription = null
    )
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
    // Шеврон выходит за коробку узла на 32 dp, и без этого поля кадр обрезал бы ровно его.
    modifier = Modifier.padding(vertical = 32.dp),
    // Через настоящий маппер, а не через свой цвет: кадр заодно проверяет, что соседние дорожки
    // получают разные оттенки.
    laneColor = fork.lane.toBranchColor(AppTheme.colors),
    contentDescription = "Ответвление",
    direction = fork.direction
  )
}

@Immutable
private data class ForkNodePreview(
  val lane: Int,
  val direction: ForkDirection
)

/**
 * Кадры превью [ForkNode]: оба направления и разные дорожки.
 *
 * Направлений ровно два, и оба обязаны быть в кадре: разворот шеврона — единственное, что их
 * различает, и ошибка в знаке видна только рядом с правильным вариантом.
 */
@Immutable
private class ForkNodePreviewProvider : PreviewParameterProvider<ForkNodePreview> {
  override val values = sequenceOf(
    ForkNodePreview(lane = 1, direction = ForkDirection.Up),
    ForkNodePreview(lane = 2, direction = ForkDirection.Down),
    ForkNodePreview(lane = 5, direction = ForkDirection.Up)
  )
}
