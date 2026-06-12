package ru.sla.clarify.uikit.component.bubble

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

@Preview
@Composable
private fun BubbleMessagePreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    BubbleMessageConversation()
  }
}

@Preview
@Composable
private fun BubbleMessagePreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    BubbleMessageConversation()
  }
}

@Composable
private fun BubbleMessageConversation(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(colors.backgroundPrimary)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Right(BubbleMessage.ReadStatus.Read),
        text = "Спасибо! Сейчас покажу",
        time = "18:05"
      )
    )
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Middle,
        side = BubbleMessage.Side.Right(BubbleMessage.ReadStatus.Sending),
        text = "Вот текущий флоу создания ветки — несколько строк, чтобы было видно перенос времени",
        time = "18:05"
      )
    )
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Bottom,
        side = BubbleMessage.Side.Right(BubbleMessage.ReadStatus.Sent),
        text = "Зажимаешь — и готово",
        time = "18:06"
      )
    )
    VSpacer(12.dp)
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Привет! Глянула макет",
        time = "18:02"
      )
    )
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Bottom,
        side = BubbleMessage.Side.Left,
        text = "Особенно как треды выносятся в отдельный экран — это топ",
        time = "18:03"
      )
    )
  }
}

@Composable
private fun ColumnScope.ClusterBubblePreview(bubble: BubbleMessage) {
  val alignment = when (bubble.side) {
    is BubbleMessage.Side.Left -> Alignment.Start
    is BubbleMessage.Side.Right -> Alignment.End
  }
  BubbleMessageItem(
    bubble = bubble,
    modifier = Modifier
      .align(alignment)
      .widthIn(max = 260.dp)
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
  val anna = BubbleMessage.Sender(
    id = UserId("anna"),
    name = "Аня Котова"
  )
  val ilya = BubbleMessage.Sender(
    id = UserId("ilya"),
    name = "Илья Соколов"
  )
  val maria = BubbleMessage.Sender(
    id = UserId("maria"),
    name = "María García с очень длинным именем"
  )
  Column(
    modifier = modifier
      .background(colors.backgroundPrimary)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Привет всем! Спасибо что собрали",
        time = "18:02",
        sender = anna
      )
    )
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Bottom,
        side = BubbleMessage.Side.Left,
        text = "Давайте сюда скидывать всё по веткам",
        time = "18:02",
        sender = anna
      )
    )
    VSpacer(12.dp)
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Ок",
        time = "18:14",
        sender = ilya
      )
    )
    VSpacer(12.dp)
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Включаюсь",
        time = "10:30",
        sender = maria
      )
    )
    VSpacer(12.dp)
    ClusterBubblePreview(
      BubbleMessage(
        id = BubbleMessage.Id(randomUuid()),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Right(BubbleMessage.ReadStatus.Read),
        text = "Супер, посмотрю после обеда и соберу мердж",
        time = "12:51",
        sender = BubbleMessage.Sender(
          id = UserId("me"),
          name = "Сергей Лановой"
        )
      )
    )
  }
}
