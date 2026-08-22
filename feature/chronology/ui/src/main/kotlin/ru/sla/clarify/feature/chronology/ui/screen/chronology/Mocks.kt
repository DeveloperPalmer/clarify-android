package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chronology.ui.components.node.DEFAULT_MY_SHARE
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
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
      time = "6 мар, 09:40",
      count = 14,
      snippet = "Ок, вынес сроки в отдельную ветку",
      myShare = 0.38f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("2"), lane = 0, gap = TimeGap.Day),
      time = "7 мар, 11:20",
      count = 6,
      snippet = "Тогда и бюджет пересчитаем",
      myShare = 0.6f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("3"), lane = 0, gap = TimeGap.Hour),
      time = "сегодня, 09:12",
      count = 4,
      snippet = "Слушай, а стикеры мы так и не сделали",
      unreadCount = 4
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("4"), lane = -1, gap = TimeGap.Minutes),
      time = "6 мар, 10:02",
      count = 9,
      snippet = "Готово, ветка тут",
      myShare = 0.2f,
      dim = true
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("5"), lane = -1, gap = TimeGap.Long),
      time = "9 мар, 18:40",
      count = 1,
      snippet = "Фиксируем 14-е",
      myShare = 1f
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
 * @param time время начала эпизода
 * @param count число сообщений в кластере
 * @param snippet последнее сообщение эпизода
 * @param myShare доля своих реплик
 * @param unreadCount счётчик непрочитанных
 * @param dim эпизод внутри слитой ветки
 */
@Immutable
internal data class MockNode(
  val node: GraphNode,
  val time: String,
  val count: Int,
  val snippet: String,
  val myShare: Float = DEFAULT_MY_SHARE,
  val unreadCount: Long = 0,
  val dim: Boolean = false
)
