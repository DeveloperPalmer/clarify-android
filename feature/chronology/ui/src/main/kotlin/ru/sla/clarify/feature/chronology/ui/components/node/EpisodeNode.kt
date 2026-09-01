package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.UnreadCountBadge
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Эпизод — кластер подряд идущих сообщений, разделённых паузой.
 *
 * Рисуется на **втором уровне детализации**, базовом: на первом узлы вырождаются в глифы, на
 * третьем видно отдельные сообщения. Уровень записан здесь, а не в имени, — перевести узел на
 * другой уровень тогда стоит правки одной строки описания, а не переименования компонента.
 *
 * Ширина фиксирована, растёт только высота: узлы стоят на дорожке, и разъезжающаяся ширина сдвигала
 * бы соседей по всей цепочке.
 *
 * @param time время начала эпизода, готовое к показу
 * @param count сколько сообщений в кластере
 * @param snippet последнее сообщение эпизода
 * @param modifier модификатор узла
 * @param myShare доля своих реплик в полоске соотношения
 * @param unreadCount счётчик непрочитанных; ноль — читать нечего
 * @param dim эпизод внутри слитой ветки
 * @param onClick тап по узлу; `null` — узел не нажимается
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EpisodeNode(
  time: String,
  count: Int,
  snippet: String,
  modifier: Modifier = Modifier,
  myShare: Float = DEFAULT_MY_SHARE,
  unreadCount: Long = 0,
  dim: Boolean = false,
  onClick: (() -> Unit)? = null
) {
  val accentColor = AppTheme.colors.contentAccentPrimary
  val unread = unreadCount > 0
  Box(
    modifier = modifier
      // Собственной подписи у узла нет: скринридер читает время, счётчик и сниппет внутри плашки.
      // Роль при этом объявляется здесь, а не берётся у `surface` ниже по цепочке: тот ставит
      // действие через `combinedClickable`, а роли не знает — без неё узел озвучивается текстом.
      .semantics { if (onClick != null) role = Role.Button }
      .graphicsLayer { alpha = if (dim) 0.6f else 1f }
      // Гало рисуется за пределами плашки и намеренно не влияет на раскладку: иначе непрочитанный
      // узел был бы шире прочитанного и сдвигал бы соседей по дорожке.
      .drawBehind { if (unread) drawUnreadHalo(accentColor, 16.dp) }
      .width(200.dp)
      .defaultMinSize(minHeight = 72.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round16,
        border = if (unread) BorderStroke(1.5.dp, accentColor) else null,
        elevation = AppTheme.elevation.small,
        onClick = onClick
      )
  ) {
    Column(
      modifier = Modifier.padding(
        start = 12.dp,
        top = 10.dp,
        end = 12.dp,
        bottom = 13.dp
      ),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      // Мета-строка переносится, а не обрезается: при крупном шрифте счётчик уходит на вторую
      // строку и узел растёт вниз — ширина у него фиксирована, ужимать текст некуда.
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          // Место под бейдж: он висит в углу плашки, и мета-строка не должна заезжать под него.
          // 24 = ширина бейджа плюс зазор.
          .padding(end = if (unread) 24.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = time,
          style = AppTheme.typography.label3Bold,
          color = AppTheme.colors.contentSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = pluralStringResource(R.plurals.chronology_episode_messages_count, count, count),
          style = AppTheme.typography.caption,
          color = AppTheme.colors.contentTertiary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = snippet,
        style = AppTheme.typography.body3,
        color = AppTheme.colors.contentSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    UnreadCountBadge(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 10.dp, end = 12.dp),
      unreadCount = unreadCount
    )
    RepliesShareBar(
      modifier = Modifier.align(Alignment.BottomCenter),
      myShare = myShare
    )
  }
}

/**
 * Соотношение реплик собеседников — вместо кадра-превью, которым вес узла показан в референсе.
 *
 * Единственное, что отличает разговор в одни ворота от диалога, пока текста в узле одна строка.
 * Убирать нельзя.
 */
@Composable
private fun RepliesShareBar(
  myShare: Float,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(3.dp)
      .background(AppTheme.colors.contentQuaternary)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(myShare.coerceIn(0f, 1f))
        .fillMaxHeight()
        .background(AppTheme.colors.contentAccentPrimary)
    )
  }
}

internal const val DEFAULT_MY_SHARE = 0.5f

@Preview
@Composable
private fun EpisodeNodePreviewLight(
  @PreviewParameter(EpisodeNodePreviewProvider::class)
  episode: EpisodeNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    EpisodeNodePreviewContent(episode)
  }
}

@Preview
@Composable
private fun EpisodeNodePreviewDark(
  @PreviewParameter(EpisodeNodePreviewProvider::class)
  episode: EpisodeNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    EpisodeNodePreviewContent(episode)
  }
}

@Composable
private fun EpisodeNodePreviewContent(episode: EpisodeNodePreview) {
  EpisodeNode(
    modifier = Modifier.padding(UNREAD_HALO_WIDTH),
    onClick = {},
    time = episode.time,
    count = episode.count,
    snippet = episode.snippet,
    myShare = episode.myShare,
    unreadCount = episode.unreadCount,
    dim = episode.dim
  )
}

@Immutable
private data class EpisodeNodePreview(
  val time: String,
  val count: Int,
  val snippet: String,
  val myShare: Float = DEFAULT_MY_SHARE,
  val unreadCount: Long = 0,
  val dim: Boolean = false
)

/**
 * Кадры превью [EpisodeNode] — по одному на состояние.
 *
 * Последнее значение проверяет обрезку: и время, и сниппет заведомо длиннее фиксированной ширины
 * узла и обязаны упереться в неё, а не растянуть плашку.
 */
@Immutable
private class EpisodeNodePreviewProvider : PreviewParameterProvider<EpisodeNodePreview> {
  override val values = sequenceOf(
    EpisodeNodePreview(
      time = "6 мар, 09:40",
      count = 14,
      snippet = "Ок, вынес сроки в отдельную ветку",
      myShare = 0.38f
    ),
    EpisodeNodePreview(
      time = "сегодня, 09:12",
      count = 4,
      snippet = "Слушай, а стикеры мы так и не сделали",
      unreadCount = 4
    ),
    EpisodeNodePreview(
      time = "1 мар, 18:03",
      count = 1,
      snippet = "Договорились",
      myShare = 1f
    ),
    EpisodeNodePreview(
      time = "28 фев, 12:00",
      count = 9,
      snippet = "Эпизод внутри слитой ветки",
      myShare = 0.2f,
      dim = true
    ),
    EpisodeNodePreview(
      time = "12 сентября 2026 года, 09:40",
      count = 24,
      snippet = "Очень длинный сниппет последнего сообщения, который обязан обрезаться эллипсисом",
      myShare = 0.7f,
      unreadCount = 128
    )
  )
}
