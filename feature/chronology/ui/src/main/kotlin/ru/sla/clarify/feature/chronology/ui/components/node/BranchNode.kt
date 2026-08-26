package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.BranchNodeStatus
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColor
import ru.sla.clarify.feature.chronology.ui.mapper.toIconResId
import ru.sla.clarify.feature.chronology.ui.mapper.toIconTint
import ru.sla.clarify.feature.chronology.ui.mapper.toLabel
import ru.sla.clarify.uikit.component.UnreadCountBadge
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Ветка как узел графа — чекпоинт, самый заметный узел после истока (§6.5 брифа).
 *
 * Рисуется на **втором уровне детализации**, базовом: на первом от узла остаётся глиф, на третьем
 * раскрытая ветка показывает свои эпизоды. Уровень записан здесь, а не в имени, — перевести узел на
 * другой уровень тогда стоит правки одной строки описания, а не переименования компонента.
 *
 * Ширина фиксирована, растёт только высота: узлы стоят цепочкой на дорожке, и разъезжающаяся ширина
 * сдвигала бы соседей.
 *
 * Закрытая и брошенная ветки узел не приглушают. По §6.8 и §12 гаснет **линия** — до 60 % и до 40 %
 * соответственно, — а плашка остаётся в полную силу: узел это вход в ветку, и закрытая тема
 * открывается так же, как живая. Бейдж непрочитанного у закрытой ветки тоже остаётся.
 *
 * @param name имя ветки; длинное обрезается эллипсисом в одну строку
 * @param laneColor цвет идентичности дорожки: какая это ветка, а не что с ней происходит (§7).
 *   Приходит готовым цветом, потому что знать свою дорожку узлу неоткуда
 * @param modifier модификатор узла
 * @param status что происходит с веткой — строка под именем
 * @param unreadCount непрочитанные в ветке; из него же выводится подсветка узла
 */
@Composable
internal fun BranchNode(
  name: String,
  laneColor: Color,
  modifier: Modifier = Modifier,
  status: BranchNodeStatus = BranchNodeStatus.Active,
  unreadCount: Long = 0
) {
  val accentColor = AppTheme.colors.contentAccentPrimary
  val unread = unreadCount > 0
  Box(
    modifier = modifier
      // Гало рисуется за пределами плашки и намеренно не влияет на раскладку: иначе непрочитанный
      // узел был бы шире прочитанного и сдвигал бы соседей по дорожке.
      .drawBehind { if (unread) drawUnreadHalo(accentColor, 16.dp) }
      .width(220.dp)
      .defaultMinSize(minHeight = 64.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round16,
        border = if (unread) BorderStroke(1.5.dp, accentColor) else null,
        elevation = AppTheme.elevation.medium
      )
      // Полоса идентичности стоит после `surface` намеренно: клип формы обрезает её по левым
      // скруглениям плашки. Подними строку выше клипа — полоса вылезет за угол прямоугольником.
      .drawBehind { drawRect(laneColor, size = Size(4.dp.toPx(), size.height)) }
  ) {
    Column(
      // Слева паддинг отсчитывается от полосы, а не от края плашки: 4 её собственных плюс 12
      // зазора, иначе имя ветки прижимается прямо к цветной кромке.
      modifier = Modifier.padding(
        start = 16.dp,
        top = 10.dp,
        end = 12.dp,
        bottom = 10.dp
      ),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        // Место под бейдж: он висит в углу плашки, и имя не должно заезжать под него.
        // 24 = ширина бейджа плюс зазор.
        modifier = Modifier.padding(end = if (unread) 24.dp else 0.dp),
        text = name,
        style = AppTheme.typography.title2Bold,
        color = AppTheme.colors.contentPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        // Иконка держится первой строки: при крупном шрифте статус переносится, и по центру двух
        // строк значок повис бы в стороне от слова, которое он обозначает.
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          modifier = Modifier.size(14.dp),
          painter = painterResource(status.toIconResId()),
          // Цвет статуса живёт в иконке, а не в тексте: золото ожидания даёт на светлой карточке
          // 2.19 : 1 и для мелкого текста запрещено §3.2 п. 4. Рядом с иконкой стоит слово, поэтому
          // цвет здесь усиливает смысл, а не несёт его в одиночку.
          tint = status.toIconTint(AppTheme.colors),
          contentDescription = null
        )
        Text(
          // Статус переносится, а не ужимается: ширина узла фиксирована, ужимать некуда, а строка,
          // потерявшая хвост, читается хуже узла, выросшего на строку.
          text = status.toLabel(),
          style = AppTheme.typography.caption,
          color = AppTheme.colors.contentTertiary
        )
      }
    }
    UnreadCountBadge(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 10.dp, end = 12.dp),
      unreadCount = unreadCount
    )
  }
}

@Preview
@Composable
private fun BranchNodePreviewLight(
  @PreviewParameter(BranchNodePreviewProvider::class)
  branch: BranchNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    BranchNodePreviewContent(branch)
  }
}

@Preview
@Composable
private fun BranchNodePreviewDark(
  @PreviewParameter(BranchNodePreviewProvider::class)
  branch: BranchNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    BranchNodePreviewContent(branch)
  }
}

@Composable
private fun BranchNodePreviewContent(branch: BranchNodePreview) {
  BranchNode(
    modifier = Modifier.padding(UNREAD_HALO_WIDTH),
    name = branch.name,
    // Через настоящий маппер, а не через свой цвет: кадр заодно проверяет, что соседние дорожки
    // действительно получают разные оттенки.
    laneColor = branch.lane.toBranchColor(AppTheme.colors),
    status = branch.status,
    unreadCount = branch.unreadCount
  )
}

@Immutable
private data class BranchNodePreview(
  val name: String,
  val lane: Int,
  val status: BranchNodeStatus = BranchNodeStatus.Active,
  val unreadCount: Long = 0
)

/**
 * Кадры превью [BranchNode] — по одному на статус.
 *
 * Последнее значение проверяет сразу две вещи: имя заведомо длиннее фиксированной ширины узла и
 * обязано упереться в неё, а трёхзначный счётчик у закрытой темы — что бейдж после слияния никуда
 * не девается.
 */
@Immutable
private class BranchNodePreviewProvider : PreviewParameterProvider<BranchNodePreview> {
  override val values = sequenceOf(
    BranchNodePreview(
      name = "Бюджет на Q3",
      lane = 1,
      unreadCount = 3
    ),
    BranchNodePreview(
      name = "Дизайн онбординга",
      lane = 2,
      status = BranchNodeStatus.Waiting,
      unreadCount = 2
    ),
    BranchNodePreview(
      name = "Сроки по релизу",
      lane = 3,
      status = BranchNodeStatus.Ready
    ),
    BranchNodePreview(
      name = "Сроки по релизу",
      lane = 3,
      status = BranchNodeStatus.Merged
    ),
    BranchNodePreview(
      name = "Стикеры",
      lane = 4,
      status = BranchNodeStatus.Abandoned(silentDays = 34)
    ),
    BranchNodePreview(
      name = "Очень длинное имя ветки, которое обязано обрезаться эллипсисом",
      lane = 5,
      status = BranchNodeStatus.Merged,
      unreadCount = 128
    )
  )
}
