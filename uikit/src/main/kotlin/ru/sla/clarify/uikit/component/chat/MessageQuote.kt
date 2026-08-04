package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.entity.MessageQuoteColors
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.resourcerefs.compose.resolveTextRef

@Composable
fun MessageQuote(
  reply: Commit.Message.Reply,
  colors: MessageQuoteColors,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  Row(
    modifier = modifier
      .height(IntrinsicSize.Max)
      .surface(
        shape = QuoteShape,
        backgroundColor = colors.background,
        onClick = onClick
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HSpacer(6.dp)
    Box(
      modifier = Modifier
        .width(3.dp)
        .fillMaxHeight()
        .padding(vertical = 6.dp)
        .clip(CircleShape)
        .background(colors.accent)
    )
    HSpacer(8.dp)
    Column(
      modifier = Modifier.padding(
        top = 6.dp,
        bottom = 6.dp,
        end = 8.dp
      )
    ) {
      reply.author?.let { author ->
        Text(
          text = resolveTextRef(author),
          style = AppTheme.typography.label3Bold,
          color = colors.author,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = reply.text,
        style = AppTheme.typography.body3,
        color = colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private val QuoteShape = RoundedCornerShape(size = 8.dp)
