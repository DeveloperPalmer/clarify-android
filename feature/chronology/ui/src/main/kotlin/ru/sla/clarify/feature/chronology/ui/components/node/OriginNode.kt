package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Исток — первое сообщение переписки и вступление, задающее тему беседы (§6.1 брифа).
 *
 * Единственный узел такого веса и **единственное место в экране с тенью 12 dp**: она максимальная в
 * системе, и оправдана здесь ровно потому, что исток один.
 *
 * В дорожках он не участвует и стоит перед графом, а не в нём, — это тот самый «якорный» вид, что
 * §6.1 разрешает только ему. Линия от заголовка экрана к истоку рисуется полотном: она соединяет
 * два элемента и ни одному из них не принадлежит.
 *
 * Текст обрезается на четвёртой строке, а не сжимается: ширина у плашки фиксирована, и первое
 * сообщение бывает любой длины. Целиком его покажет превью-карточка узла.
 *
 * @param text первое сообщение переписки целиком
 * @param date дата истока, уже приведённая к локальному времени устройства
 * @param myName моё имя — из него берутся инициалы, когда фото нет
 * @param myPhotoUrl моё фото или `null`
 * @param peerName имя собеседника
 * @param peerPhotoUrl фото собеседника или `null`
 * @param modifier модификатор узла
 */
@Composable
internal fun OriginNode(
  text: String,
  date: String,
  myName: String,
  myPhotoUrl: String?,
  peerName: String,
  peerPhotoUrl: String?,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .width(280.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round24,
        elevation = AppTheme.elevation.largest
      )
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      // Отрицательный зазор и есть перекрытие аватаров: собеседник рисуется вторым и потому лежит
      // поверх моего, как на макете.
      horizontalArrangement = Arrangement.spacedBy(-12.dp)
    ) {
      Avatar(
        size = 40.dp,
        photoUrl = myPhotoUrl,
        fallback = myName
      )
      Avatar(
        size = 40.dp,
        photoUrl = peerPhotoUrl,
        fallback = peerName
      )
    }
    Text(
      text = text,
      style = AppTheme.typography.body1,
      color = AppTheme.colors.contentPrimary,
      maxLines = 4,
      overflow = TextOverflow.Ellipsis
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = date,
        style = AppTheme.typography.caption,
        color = AppTheme.colors.contentTertiary
      )
      Text(
        // Капслок применяется здесь, а не в стиле: `overline` задаёт только метрики разряжённого
        // начертания, а решение кричать принимает точка использования.
        text = stringResource(R.string.chronology_origin_caption).uppercase(),
        style = AppTheme.typography.overline,
        // Читаемый вариант акцента, а не `contentAccentPrimary`, которым §6.1 брифа описывал эту
        // подпись: #7520FF на тёмной карточке даёт 2.27 : 1 и для текста не годится (§3.2 п. 4).
        color = AppTheme.colors.contentAccentReadable
      )
    }
  }
}

@Preview
@Composable
private fun OriginNodePreviewLight(
  @PreviewParameter(OriginNodePreviewProvider::class)
  origin: OriginNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    OriginNodePreviewContent(origin)
  }
}

@Preview
@Composable
private fun OriginNodePreviewDark(
  @PreviewParameter(OriginNodePreviewProvider::class)
  origin: OriginNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    OriginNodePreviewContent(origin)
  }
}

@Composable
private fun OriginNodePreviewContent(origin: OriginNodePreview) {
  OriginNode(
    modifier = Modifier.padding(4.dp),
    text = origin.text,
    date = origin.date,
    myName = "Вы",
    myPhotoUrl = null,
    peerName = "Анна Ковалёва",
    peerPhotoUrl = null
  )
}

@Immutable
private data class OriginNodePreview(
  val text: String,
  val date: String
)

/**
 * Кадры превью [OriginNode]: макетный, вырожденный и переполненный.
 *
 * Второй проверяет, что на одной короткой строке карточка не разваливается, — исток бывает и таким;
 * третий, что текст упирается в четвёртую строку и обрезается, а не растит плашку до бесконечности.
 *
 * Фото у всех кадров нет намеренно: в превью его всё равно нечем загрузить, а инициалы показывают
 * ровно то, что увидит большинство переписок.
 */
@Immutable
private class OriginNodePreviewProvider : PreviewParameterProvider<OriginNodePreview> {
  override val values = sequenceOf(
    OriginNodePreview(
      text = "Привет! Смотри, я вынес правки по релизу в отдельную тему — так проще не потерять.",
      date = "3 марта, 09:41"
    ),
    OriginNodePreview(
      text = "Привет",
      date = "12 января, 22:07"
    ),
    OriginNodePreview(
      text = "Первое сообщение бывает и таким: человек вываливает всё, что накопилось, одним " +
        "куском — план на квартал, список претензий и три вопроса, на которые ответа он не ждёт, " +
        "потому что уже решил, как будет. Читать это целиком придётся в превью-карточке узла.",
      date = "28 февраля, 08:15"
    )
  )
}
