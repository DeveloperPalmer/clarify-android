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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.MessageNodeState
import ru.sla.clarify.feature.chronology.ui.mapper.toIconResId
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
 */
@Composable
internal fun MessageNode(
  text: String,
  isMine: Boolean,
  modifier: Modifier = Modifier,
  state: MessageNodeState = MessageNodeState.Normal
) {
  val background = if (isMine) {
    AppTheme.colors.backgroundAccentPrimary
  } else {
    AppTheme.colors.cardSecondary
  }
  Row(
    modifier = modifier
      .graphicsLayer { alpha = if (state == MessageNodeState.Sending) SENDING_ALPHA else 1f }
      .widthIn(max = MAX_WIDTH)
      .defaultMinSize(minHeight = MIN_HEIGHT)
      .background(background, AppTheme.shapes.round12)
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
        modifier = Modifier.size(ICON_SIZE),
        painter = painterResource(iconResId),
        tint = AppTheme.colors.contentTertiary,
        contentDescription = null
      )
    }
  }
}

private const val SENDING_ALPHA = 0.6f
private val MIN_HEIGHT: Dp = 28.dp
private val MAX_WIDTH: Dp = 180.dp
private val ICON_SIZE: Dp = 12.dp

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
      state = message.state
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
      state = message.state
    )
  }
}
