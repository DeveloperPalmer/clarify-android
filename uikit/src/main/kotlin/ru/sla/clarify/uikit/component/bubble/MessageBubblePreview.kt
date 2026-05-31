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
  BubbleMessage(
    bubble = bubble,
    modifier = Modifier
      .align(alignment)
      .widthIn(max = 260.dp)
  )
}
