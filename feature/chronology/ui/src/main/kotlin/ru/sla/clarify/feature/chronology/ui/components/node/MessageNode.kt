package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.MessageNodeState
import ru.sla.clarify.feature.chronology.ui.mapper.toIconResId
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

/**
 * Отдельное сообщение как узел графа — самый мелкий вес узла на полотне.
 *
 * Рисуется на **третьем уровне детализации**: на первом узлы вырождаются в глифы, на втором дорожка
 * собрана в эпизоды, и только на третьем видно отдельные сообщения. Уровень записан здесь, а не в
 * имени, — перевести узел на другой уровень тогда стоит правки одной строки описания, а не
 * переименования компонента и всех ссылок на него.
 *
 * Заливка `cardSecondary` у сообщения собеседника здесь допустима: узел лежит на полотне, а не на
 * карточке, — в отличие от узлов покрупнее, которым в светлой теме `cardSecondary` совпал бы с
 * фоном.
 *
 * @param text текст сообщения; длинное обрезается эллипсисом
 * @param isMine моё сообщение или собеседника — от этого зависит заливка
 * @param contentDescription связная подпись для скринридера (§14): собирается маппером, потому что
 *   имя ветки узлу неоткуда взять
 * @param modifier модификатор узла
 * @param state отредактировано, с цитатой или отправляется
 * @param onClick тап по узлу; `null` — узел не нажимается
 */
@Composable
internal fun MessageNode(
  text: String,
  isMine: Boolean,
  contentDescription: String,
  modifier: Modifier = Modifier,
  state: MessageNodeState = MessageNodeState.Normal,
  onClick: (() -> Unit)? = null
) {
  val background = if (isMine) {
    AppTheme.colors.backgroundAccentPrimary
  } else {
    AppTheme.colors.cardSecondary
  }
  Row(
    modifier = modifier
      // Подпись накрывает и текст, и значок состояния: скринридеру нужна одна фраза, а не строка
      // сообщения отдельно от иконки карандаша.
      .clearAndSetSemantics {
        this.contentDescription = contentDescription
        if (onClick != null) role = Role.Button
      }
      .graphicsLayer { alpha = if (state == MessageNodeState.Sending) 0.6f else 1f }
      .widthIn(max = 180.dp)
      .defaultMinSize(minHeight = 28.dp)
      // `surface`, а не `background`: тень при нулевой высоте ничего не рисует, зато клип и клик
      // приходят готовыми и ровно теми же, что у остальных плашек.
      .surface(
        backgroundColor = background,
        shape = AppTheme.shapes.round12,
        onClick = onClick
      )
      .padding(horizontal = 14.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      text = text,
      style = AppTheme.typography.label3,
      color = AppTheme.colors.contentPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    val iconResId = state.toIconResId()
    if (iconResId != null) {
      Icon(
        modifier = Modifier.size(12.dp),
        painter = painterResource(iconResId),
        tint = AppTheme.colors.contentTertiary,
        contentDescription = null
      )
    }
  }
}

@Preview
@Composable
private fun MessageNodePreviewLight(
  @PreviewParameter(MessageNodePreviewProvider::class)
  message: MessageNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    MessageNode(
      text = message.text,
      isMine = message.isMine,
      contentDescription = message.text,
      state = message.state,
      onClick = {}
    )
  }
}

@Preview
@Composable
private fun MessageNodePreviewDark(
  @PreviewParameter(MessageNodePreviewProvider::class)
  message: MessageNodePreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    MessageNode(
      text = message.text,
      isMine = message.isMine,
      contentDescription = message.text,
      state = message.state,
      onClick = {}
    )
  }
}

@Immutable
private data class MessageNodePreview(
  val text: String,
  val isMine: Boolean,
  val state: MessageNodeState = MessageNodeState.Normal
)

/**
 * Кадры превью [MessageNode] — по одному на состояние.
 *
 * Последнее значение проверяет обрезку: текст заведомо длиннее максимальной ширины узла и обязан
 * упереться в неё, а не растянуть плашку.
 */
@Immutable
private class MessageNodePreviewProvider : PreviewParameterProvider<MessageNodePreview> {
  override val values = sequenceOf(
    MessageNodePreview(
      text = "Не бьётся по срокам",
      isMine = false
    ),
    MessageNodePreview(
      text = "Где именно?",
      isMine = true
    ),
    MessageNodePreview(
      text = "Выношу в ветку",
      isMine = true,
      state = MessageNodeState.Edited
    ),
    MessageNodePreview(
      text = "Готово, ветка тут",
      isMine = false,
      state = MessageNodeState.Quoted
    ),
    MessageNodePreview(
      text = "Фиксируем 14-е",
      isMine = true,
      state = MessageNodeState.Sending
    ),
    MessageNodePreview(
      text = "Очень длинный текст сообщения, который обязан обрезаться эллипсисом",
      isMine = false
    )
  )
}
