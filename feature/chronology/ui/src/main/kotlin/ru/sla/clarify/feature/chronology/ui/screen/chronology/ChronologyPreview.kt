package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chronology.domain.ChronologyEdge
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph
import ru.sla.clarify.feature.chronology.domain.ChronologyNode
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import java.time.Instant
import java.time.ZoneId

@Preview
@Composable
private fun ChronologyReadyContentPreview(
  @PreviewParameter(ChronologyPreviewProvider::class) state: ChronologyPreviewState
) {
  AppTheme(currentTheme = state.theme) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.backgroundPrimary)
    ) {
      ChronologyReadyContent(
        peerId = state.peerId,
        graph = state.graph,
        onBack = {}
      )
    }
  }
}

internal data class ChronologyPreviewState(
  val label: String,
  val theme: ColorTheme,
  val peerId: String,
  val graph: ChronologyGraph
) {
  override fun toString(): String = label
}

internal class ChronologyPreviewProvider : PreviewParameterProvider<ChronologyPreviewState> {
  override val values: Sequence<ChronologyPreviewState> = sequenceOf(
    ChronologyPreviewState(
      label = "Light • branched",
      theme = ColorTheme.Light,
      peerId = "Алиса",
      graph = sampleBranchedGraph()
    ),
    ChronologyPreviewState(
      label = "Dark • branched",
      theme = ColorTheme.Dark,
      peerId = "Алиса",
      graph = sampleBranchedGraph()
    ),
    ChronologyPreviewState(
      label = "Empty",
      theme = ColorTheme.Light,
      peerId = "Боб",
      graph = ChronologyGraph.Empty
    )
  )
}

private fun sampleBranchedGraph(): ChronologyGraph {
  // depth=0 : корень "m1"
  // depth=1 : "m2" (ответ на m1) и "m3" (другой ответ на m1) — две ветки
  // depth=2 : "m4" (ответ на m2), "m5" (ответ на m3)
  // depth=3 : "m6" (ответ на m5)
  val now = Instant.ofEpochSecond(PREVIEW_NOW_EPOCH_SECONDS)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  val messages = listOf(
    sampleMessage(
      id = ChatMessage.Id("m1"),
      parentId = null,
      text = "Привет! Как дела?",
      isSelf = false,
      offsetSeconds = -SEC_30_MIN
    ),
    sampleMessage(
      id = ChatMessage.Id("m2"),
      parentId = ChatMessage.Id("m4"),
      text = "Отлично, готовлю релиз",
      isSelf = true,
      offsetSeconds = -SEC_25_MIN
    ),
    sampleMessage(
      id = ChatMessage.Id("m3"),
      parentId = ChatMessage.Id("m4"),
      text = "Кстати, что насчёт встречи?",
      isSelf = true,
      offsetSeconds = -SEC_20_MIN
    ),
    sampleMessage(
      id = ChatMessage.Id("m4"),
      parentId = ChatMessage.Id("m4"),
      text = "Здорово! Поделишься скрином?",
      isSelf = false,
      offsetSeconds = -SEC_15_MIN
    ),
    sampleMessage(
      id = ChatMessage.Id("m6"),
      parentId = ChatMessage.Id("m5"),
      text = "Давай завтра в 10",
      isSelf = false,
      offsetSeconds = -SEC_10_MIN
    ),
    sampleMessage(
      id = ChatMessage.Id("m6"),
      parentId = ChatMessage.Id("m5"),
      text = "Договорились",
      isSelf = true,
      offsetSeconds = -SEC_5_MIN
    )
  ).map { (msg, offsetSeconds) ->
    msg.copy(timestamp = now.plusSeconds(offsetSeconds))
  }

  val nodes = listOf(
    ChronologyNode(messages[0], depth = 0, branchIndex = 0),
    ChronologyNode(messages[1], depth = 1, branchIndex = 0),
    ChronologyNode(messages[2], depth = 1, branchIndex = 1),
    ChronologyNode(messages[3], depth = 2, branchIndex = 0),
    ChronologyNode(messages[4], depth = 2, branchIndex = 1),
    ChronologyNode(messages[5], depth = 3, branchIndex = 0)
  )
  val edges = listOf(
    ChronologyEdge("m1", "m2"),
    ChronologyEdge("m1", "m3"),
    ChronologyEdge("m2", "m4"),
    ChronologyEdge("m3", "m5"),
    ChronologyEdge("m5", "m6")
  )
  return ChronologyGraph(nodes = nodes, edges = edges)
}

private fun sampleMessage(
  id: ChatMessage.Id,
  parentId: ChatMessage.Id?,
  text: String,
  isSelf: Boolean,
  offsetSeconds: Long
): Pair<ChatMessage, Long> {
  return ChatMessage(
    id = id,
    parentId = parentId,
    peerId = "Алиса",
    senderId = if (isSelf) "me" else "alice",
    text = text,
    // timestamp заполнится позже относительно "сейчас" в превью
    timestamp = Instant.ofEpochSecond(PREVIEW_NOW_EPOCH_SECONDS)
      .atZone(ZoneId.systemDefault())
      .toLocalDateTime(),
    isSelf = isSelf,
    status = ChatMessage.Status.Sent,
    colorHex = ""
  ) to offsetSeconds
}

private const val PREVIEW_NOW_EPOCH_SECONDS = 1_715_000_000L
private const val SEC_5_MIN = 5L * 60L
private const val SEC_10_MIN = 10L * 60L
private const val SEC_15_MIN = 15L * 60L
private const val SEC_20_MIN = 20L * 60L
private const val SEC_25_MIN = 25L * 60L
private const val SEC_30_MIN = 30L * 60L
