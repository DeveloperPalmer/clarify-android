package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.MessageNodeState
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Временный набор узлов вместо данных.
 *
 * Задаётся дорожками и паузами, а не координатами: положение считает геометрия полотна, и это
 * заодно проверяет, что ось X действительно выводится из паузы. Уезжает целиком, вместе с этим
 * файлом, как только появится сборка графа из веток и коммитов.
 *
 * @return узлы демо-графа в хронологическом порядке
 */
internal fun mockNodes(): List<MockNode> {
  return listOf(
    MockNode(
      node = GraphNode(id = GraphNode.Id("1"), lane = 0, gap = TimeGap.Hours),
      text = "Не бьётся по срокам",
      isMine = false
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("2"), lane = 0, gap = TimeGap.Minutes),
      text = "Где именно?",
      isMine = true
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("3"), lane = 0, gap = TimeGap.Hour),
      text = "Выношу в ветку",
      isMine = true,
      state = MessageNodeState.Edited
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("4"), lane = -1, gap = TimeGap.Minutes),
      text = "Готово, ветка тут",
      isMine = false,
      state = MessageNodeState.Quoted
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("5"), lane = -1, gap = TimeGap.Day),
      text = "Фиксируем 14-е",
      isMine = true,
      state = MessageNodeState.Sending
    )
  )
}

/**
 * Временный узел демо-графа: раскладочная часть плюс то, что нарисовано внутри плашки.
 *
 * Существует ровно до появления сборки графа из веток и коммитов — тогда содержимое узла придёт из
 * домена, а не из литералов экрана.
 *
 * @param node раскладочная часть: дорожка и пауза
 * @param text текст сообщения
 * @param isMine своё сообщение или собеседника
 * @param state состояние плашки: иконка и прозрачность
 */
@Immutable
internal data class MockNode(
  val node: GraphNode,
  val text: String,
  val isMine: Boolean,
  val state: MessageNodeState = MessageNodeState.Normal
)
