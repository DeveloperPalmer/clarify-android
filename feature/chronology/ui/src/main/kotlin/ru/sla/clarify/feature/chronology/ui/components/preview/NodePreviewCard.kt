package ru.sla.clarify.feature.chronology.ui.components.preview

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.NodePreview
import ru.sla.clarify.uikit.animation.LocalSharedTransitionScope
import ru.sla.clarify.uikit.animation.SharedContainer
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Превью-карточка узла — то, во что узел морфится по тапу (§11.2 брифа).
 *
 * Карточка — приёмник общего элемента, а источник у неё не узел, а якорь: узел живёт внутри слоя
 * камеры, границ которого общий элемент не видит вовсе. Подробности — в [NodePreviewMorph].
 *
 * Содержимое проявляется не сразу, а во второй половине морфа: пока поверхность растёт, текст в ней
 * читать всё равно нельзя, а проявившись в начале, он ехал бы вместе с границами. Окно то же, что у
 * карточки merge request, — и это не совпадение, а единственный способ, чтобы два морфа в одном
 * приложении выглядели одним приёмом.
 *
 * **Кнопки «Открыть в чате» здесь нет.** §11.2 её требует, и она приедет вместе со сборкой графа из
 * домена: пока узел — это строка демо-набора, а не `Commit.Id`, вести ей некуда, а подсветка в чате
 * ищет коммит среди загруженных и не нашла бы ничего.
 *
 * @param preview что показать: автор, время и полный текст
 * @param visible открыта ли карточка; на `false` она морфится обратно в узел
 * @param morphedCorner скругление плашки, из которой карточка выросла: к нему она возвращается
 * @param modifier модификатор карточки
 */
@Composable
internal fun NodePreviewCard(
  preview: NodePreview,
  visible: Boolean,
  morphedCorner: Dp,
  modifier: Modifier = Modifier
) {
  SharedContainer(
    modifier = modifier,
    key = NODE_PREVIEW_MOTION_KEY,
    visible = visible,
    restingCorner = NODE_PREVIEW_CARD_CORNER,
    morphedCorner = morphedCorner
  ) {
    Box(
      modifier = Modifier.sharedSurface(
        color = AppTheme.colors.cardPrimary,
        elevation = AppTheme.elevation.largest
      )
    ) {
      Column(
        modifier = Modifier
          .revealContent(NODE_PREVIEW_CARD_REVEAL_WINDOW)
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Avatar(
            size = 40.dp,
            photoUrl = preview.authorPhotoUrl,
            fallback = preview.authorName
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = preview.authorName,
              style = AppTheme.typography.title3Bold,
              color = AppTheme.colors.contentPrimary
            )
            Text(
              text = preview.time,
              style = AppTheme.typography.caption,
              color = AppTheme.colors.contentTertiary
            )
          }
        }
        Text(
          // Текст не обрезается: карточка для того и открывается, чтобы прочитать целиком то, что
          // в плашке уместилось одной строкой с эллипсисом.
          text = preview.text,
          style = AppTheme.typography.body1,
          color = AppTheme.colors.contentPrimary
        )
      }
    }
  }
}

@Preview
@Composable
private fun NodePreviewCardPreviewLight(
  @PreviewParameter(NodePreviewCardPreviewProvider::class)
  preview: NodePreviewCardPreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    NodePreviewCardPreviewContent(preview)
  }
}

@Preview
@Composable
private fun NodePreviewCardPreviewDark(
  @PreviewParameter(NodePreviewCardPreviewProvider::class)
  preview: NodePreviewCardPreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    NodePreviewCardPreviewContent(preview)
  }
}

/**
 * Кадр карточки вне экрана.
 *
 * Своя [SharedTransitionLayout] здесь нужна потому, что настоящую поднимает `MainActivity`, а превью
 * до неё не достаёт: без области общих элементов `sharedSurface` падает на первом же кадре.
 */
@Composable
private fun NodePreviewCardPreviewContent(preview: NodePreviewCardPreview) {
  SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
      NodePreviewCard(
        modifier = Modifier.padding(12.dp),
        preview = preview,
        visible = true,
        morphedCorner = 16.dp
      )
    }
  }
}

@Immutable
private data class NodePreviewCardPreview(
  override val authorName: String,
  override val authorPhotoUrl: String?,
  override val text: String,
  override val time: String
) : NodePreview

/**
 * Кадры превью [NodePreviewCard] — по одному на то, чем карточки отличаются.
 *
 * Второй кадр проверяет длинный текст: обрезки у карточки нет вовсе, и она обязана расти вниз, а не
 * упираться в свою высоту.
 */
@Immutable
private class NodePreviewCardPreviewProvider : PreviewParameterProvider<NodePreviewCardPreview> {
  override val values = sequenceOf(
    NodePreviewCardPreview(
      authorName = "Анна Ковалёва",
      authorPhotoUrl = null,
      text = "Ок, вынес сроки в отдельную ветку",
      time = "6 марта, 09:40"
    ),
    NodePreviewCardPreview(
      authorName = "Вы",
      authorPhotoUrl = null,
      text = "Давай так: я до конца недели собираю смету по трём подрядчикам, ты пока " +
        "уточняешь у юристов, проходит ли у нас рамочный договор. В понедельник сверимся и " +
        "решим, идём мы в этот квартал или переносим.",
      time = "7 марта, 18:03"
    )
  )
}
