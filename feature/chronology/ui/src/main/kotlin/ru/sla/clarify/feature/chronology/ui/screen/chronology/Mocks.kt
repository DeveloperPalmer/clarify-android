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
 * Дорожек намеренно больше, чем помещается по высоте экрана. Пока их было две, содержимое влезало
 * во вьюпорт целиком, вертикальный диапазон камеры схлопывался в точку — и ни вертикального
 * панорамирования, ни диагонального броска на экране просто не существовало, сколько ни води
 * пальцем. Убавляя набор, надо помнить, что вместе с ним пропадёт и половина проверяемого поведения.
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
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("6"), lane = -2, gap = TimeGap.Hour),
      time = "6 мар, 12:15",
      count = 22,
      snippet = "Дизайн отдали на ревью",
      myShare = 0.45f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("7"), lane = -2, gap = TimeGap.Day),
      time = "8 мар, 10:30",
      count = 3,
      snippet = "Правки внесены, смотрите",
      myShare = 0.7f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("8"), lane = -3, gap = TimeGap.Hours),
      time = "6 мар, 16:48",
      count = 5,
      snippet = "Созвон перенесли на четверг",
      myShare = 0.5f,
      dim = true
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("9"), lane = -4, gap = TimeGap.Minutes),
      time = "6 мар, 17:03",
      count = 11,
      snippet = "Скинул смету, посмотри цифры",
      myShare = 0.15f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("10"), lane = -4, gap = TimeGap.Long),
      time = "10 мар, 08:20",
      count = 2,
      snippet = "Смета согласована",
      myShare = 0.9f,
      unreadCount = 2
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("11"), lane = -5, gap = TimeGap.Hour),
      time = "7 мар, 09:05",
      count = 7,
      snippet = "Тут ещё вопрос по логотипу",
      myShare = 0.33f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("12"), lane = -6, gap = TimeGap.Hours),
      time = "7 мар, 14:40",
      count = 18,
      snippet = "Обсудили шрифты, остановились на втором",
      myShare = 0.55f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("13"), lane = -7, gap = TimeGap.Day),
      time = "8 мар, 19:12",
      count = 4,
      snippet = "Архив со старыми макетами",
      myShare = 0.25f,
      dim = true
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("14"), lane = 1, gap = TimeGap.Minutes),
      time = "6 мар, 09:55",
      count = 8,
      snippet = "Я про интеграцию, а не про экспорт",
      myShare = 0.62f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("15"), lane = 1, gap = TimeGap.Hours),
      time = "6 мар, 21:30",
      count = 13,
      snippet = "Ключи выдам завтра утром",
      myShare = 0.4f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("16"), lane = 2, gap = TimeGap.Hour),
      time = "7 мар, 10:10",
      count = 6,
      snippet = "Тесты на стейджинге зелёные",
      myShare = 0.8f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("17"), lane = 3, gap = TimeGap.Day),
      time = "8 мар, 11:45",
      count = 9,
      snippet = "Прод выкатили, полёт нормальный",
      myShare = 0.5f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("18"), lane = 3, gap = TimeGap.Minutes),
      time = "8 мар, 11:58",
      count = 1,
      snippet = "Ура",
      myShare = 1f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("19"), lane = 4, gap = TimeGap.Hours),
      time = "8 мар, 16:20",
      count = 15,
      snippet = "Метрики за неделю в таблице",
      myShare = 0.3f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("20"), lane = 5, gap = TimeGap.Long),
      time = "11 мар, 09:00",
      count = 3,
      snippet = "Планы на следующий спринт",
      myShare = 0.66f,
      unreadCount = 3
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("21"), lane = 6, gap = TimeGap.Hour),
      time = "11 мар, 12:35",
      count = 5,
      snippet = "Отпуск с 20-го, напоминаю",
      myShare = 0.44f
    ),
    MockNode(
      node = GraphNode(id = GraphNode.Id("22"), lane = 0, gap = TimeGap.Day),
      time = "12 мар, 08:15",
      count = 2,
      snippet = "Возвращаемся к срокам",
      myShare = 0.5f,
      unreadCount = 1
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
