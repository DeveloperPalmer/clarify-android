package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.AppTheme.colors
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.strRef
import java.time.LocalDateTime
import ru.sla.clarify.entity.chat.Commit as DomainCommit

@Preview
@Composable
private fun MessagePreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    MessagePreviewContent()
  }
}

@Preview
@Composable
private fun MessagePreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    MessagePreviewContent()
  }
}

@Composable
private fun MessagePreviewContent(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(colors.backgroundPrimary)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Top,
        side = Commit.Message.Side.Right(Commit.Message.ReadStatus.Read),
        text = "Спасибо! Сейчас покажу",
        time = "18:05"
      )
    )
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Middle,
        side = Commit.Message.Side.Right(Commit.Message.ReadStatus.Sending),
        text = "Вот текущий флоу создания ветки — несколько строк, чтобы было видно перенос времени",
        time = "18:05"
      )
    )
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Bottom,
        side = Commit.Message.Side.Right(Commit.Message.ReadStatus.Sent),
        text = "Зажимаешь — и готово",
        time = "18:06",
        edited = true
      )
    )
    VSpacer(12.dp)
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Top,
        side = Commit.Message.Side.Left,
        text = "Привет! Глянула макет",
        time = "18:02"
      )
    )
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Bottom,
        side = Commit.Message.Side.Left,
        text = "Особенно как треды выносятся в отдельный экран — это топ",
        time = "18:03"
      )
    )
    VSpacer(12.dp)
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Top,
        side = Commit.Message.Side.Right(Commit.Message.ReadStatus.Read),
        text = "Уже поправил — скинул новый скрин выше",
        time = "18:07",
        replyCommit = previewReply(
          author = "Аня Котова",
          text = "На превью кнопка прижата к картинке — снизу не хватает отступа"
        )
      )
    )
    MessageContent(
      previewMessage(
        shape = Commit.Message.Shape.Bottom,
        side = Commit.Message.Side.Left,
        text = "Ок",
        time = "18:08",
        replyCommit = previewReply(
          author = "Вы",
          text = "Уже поправил — скинул новый скрин выше"
        )
      )
    )
  }
}

@Composable
private fun ColumnScope.MessageContent(message: Commit.Message) {
  val alignment = when (message.side) {
    is Commit.Message.Side.Left -> Alignment.Start
    is Commit.Message.Side.Right -> Alignment.End
  }
  Message(
    modifier = Modifier
      .align(alignment)
      .widthIn(max = 260.dp),
    message = message,
    selectionEnabled = false,
    onClick = {},
    onLongClick = {},
    onAnchorBounds = {}
  )
}

@Preview
@Composable
private fun GroupBubbleMessagePreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    GroupBubbleMessageConversation()
  }
}

@Preview
@Composable
private fun GroupBubbleMessagePreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    GroupBubbleMessageConversation()
  }
}

@Composable
private fun GroupBubbleMessageConversation(modifier: Modifier = Modifier) {
  val anna = Commit.Message.Sender(
    id = UserId("anna"),
    name = "Аня Котова"
  )
  val ilya = Commit.Message.Sender(
    id = UserId("ilya"),
    name = "Илья Соколов"
  )
  val maria = Commit.Message.Sender(
    id = UserId("maria"),
    name = "María García с очень длинным именем"
  )
  Column(
    modifier = modifier
      .background(colors.backgroundPrimary)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    MessageContent(
      previewMessage(
        side = Commit.Message.Side.Left,
        shape = Commit.Message.Shape.Top,
        text = "Привет всем! Спасибо что собрали",
        time = "18:02",
        sender = anna
      )
    )
    MessageContent(
      previewMessage(
        side = Commit.Message.Side.Left,
        shape = Commit.Message.Shape.Bottom,
        text = "Давайте сюда скидывать всё по веткам",
        time = "18:02",
        sender = anna
      )
    )
    VSpacer(12.dp)
    MessageContent(
      previewMessage(
        side = Commit.Message.Side.Left,
        shape = Commit.Message.Shape.Top,
        text = "Ок",
        time = "18:14",
        sender = ilya
      )
    )
    VSpacer(12.dp)
    MessageContent(
      previewMessage(
        side = Commit.Message.Side.Left,
        shape = Commit.Message.Shape.Top,
        text = "Включаюсь",
        time = "10:30",
        sender = maria
      )
    )
    VSpacer(12.dp)
    MessageContent(
      previewMessage(
        side = Commit.Message.Side.Right(Commit.Message.ReadStatus.Read),
        shape = Commit.Message.Shape.Top,
        text = "Супер, посмотрю после обеда и соберу мердж",
        time = "12:51",
        sender = Commit.Message.Sender(
          id = UserId("me"),
          name = "Сергей Лановой"
        )
      )
    )
  }
}

private fun previewReply(
  author: String,
  text: String
): Commit.Message.Reply {
  return Commit.Message.Reply(
    targetId = DomainCommit.Id(randomUuid()),
    author = strRef(author),
    text = text
  )
}

private fun previewMessage(
  side: Commit.Message.Side,
  shape: Commit.Message.Shape,
  text: String,
  time: String,
  sender: Commit.Message.Sender? = null,
  replyCommit: Commit.Message.Reply? = null,
  edited: Boolean = false,
  selected: Boolean = false
): Commit.Message {
  val id = randomUuid()
  return Commit.Message(
    key = "message:$id",
    source = DomainCommit.Message(
      id = DomainCommit.Id(id),
      timestamp = LocalDateTime.now(),
      senderId = sender?.id ?: UserId(id),
      text = text,
      isSelf = side is Commit.Message.Side.Right,
      status = DomainCommit.Status.Sent
    ),
    text = text,
    side = side,
    shape = shape,
    time = time,
    sender = sender,
    replyCommit = replyCommit,
    edited = edited,
    selected = selected
  )
}
