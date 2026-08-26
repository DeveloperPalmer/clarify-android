package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chronology.ui.components.node.DEFAULT_MY_SHARE
import ru.sla.clarify.feature.chronology.ui.entity.EpisodeContent
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Временный набор веток вместо данных.
 *
 * **Шесть веток, и число выбрано не для полноты картины:** оттенков идентичности ровно шесть, и
 * шестая ветка сторожит ошибку, при которой остаток от деления давал ноль и красил её цветом
 * магистрали.
 *
 * Каждая ветка отвечает за своё состояние из §6.8 брифа, и вместе они покрывают всё, что рисуется
 * по-разному: слитую линию, пунктир открытого merge request, готовность к слиянию, брошенную тему и
 * две живые. Ветка `design` при этом стоит здесь ради одного-единственного свойства — она уходит с
 * магистрали **между последним сообщением ветки `terms` и её слиянием**, и на этом месте раскладка
 * ломалась дважды: считая занятость дорожки по узлам ветки, обе получали бы одну дорожку, и
 * горизонталь возврата `terms` прошла бы сквозь плашки `design`.
 *
 * @return ветки демо-графа в порядке ветвления
 */
internal fun mockBranches(): List<GraphBranch> {
  return listOf(
    GraphBranch(
      id = GraphBranch.Id("terms"),
      colorIndex = 1,
      forkedFrom = GraphNode.Id("fork-terms"),
      mergedAt = GraphNode.Id("merge-terms"),
      status = GraphBranchStatus.Merged
    ),
    GraphBranch(
      id = GraphBranch.Id("export"),
      colorIndex = 2,
      forkedFrom = GraphNode.Id("fork-export"),
      mergedAt = null,
      status = GraphBranchStatus.Alive
    ),
    GraphBranch(
      id = GraphBranch.Id("design"),
      colorIndex = 3,
      forkedFrom = GraphNode.Id("fork-design"),
      mergedAt = null,
      status = GraphBranchStatus.Waiting
    ),
    GraphBranch(
      id = GraphBranch.Id("budget"),
      colorIndex = 4,
      forkedFrom = GraphNode.Id("fork-budget"),
      mergedAt = null,
      status = GraphBranchStatus.Ready
    ),
    GraphBranch(
      id = GraphBranch.Id("logo"),
      colorIndex = 5,
      forkedFrom = GraphNode.Id("fork-logo"),
      mergedAt = null,
      status = GraphBranchStatus.Abandoned
    ),
    GraphBranch(
      id = GraphBranch.Id("release"),
      colorIndex = 6,
      forkedFrom = GraphNode.Id("fork-release"),
      mergedAt = null,
      status = GraphBranchStatus.Alive
    )
  )
}

/**
 * Временный набор узлов вместо данных.
 *
 * Задаётся ветками и паузами, а не координатами: положение считает геометрия полотна, дорожку —
 * раскраска по занятости, и это заодно проверяет, что ни то, ни другое не подменено литералом.
 * Уезжает целиком, вместе с этим файлом, как только появится сборка графа из веток и коммитов.
 *
 * **Узлы идут строго по времени, и порядок здесь не косметика.** Ось X накапливается по порядку
 * списка, поэтому набор, сгруппированный по веткам, ставил бы ветку от 8 марта левее ветки от
 * 6-го — прямое нарушение §4.1 брифа. Пауза каждого узла посчитана от предыдущего **по времени**, а
 * не назначена на глаз; все пять ступеней `TimeGap` в наборе встречаются.
 *
 * **Точки ветвления и слияния — настоящие узлы списка, а не отметки на линии.** На этом держится
 * то, что вертикаль ребра не может пересечь чужую плашку: накопительная ось даёт каждому узлу
 * собственный отрезок по X, и соседи лежат либо левее, либо правее — но не под ним.
 *
 * Последний узел ветки `export` стоит **позже** последнего узла магистрали, и это тоже сторож:
 * живая тема, идущая после того, как магистраль замолчала, — обычное состояние, на котором
 * хвост, привязанный к фронту, уходил бы в отрицательную длину.
 *
 * @return узлы демо-графа в хронологическом порядке
 */
internal fun mockNodes(): List<MockNode> {
  return listOf(
    episode(
      id = "1",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "6 мар, 09:40",
      count = 14,
      snippet = "Ок, давай по порядку",
      myShare = 0.38f
    ),
    fork(id = "fork-terms", gap = TimeGap.Minutes),
    episode(
      id = "2",
      branchId = "terms",
      gap = TimeGap.Hour,
      time = "6 мар, 09:52",
      count = 8,
      snippet = "Сроки я вынес сюда",
      myShare = 0.62f
    ),
    episode(
      id = "3",
      branchId = "terms",
      gap = TimeGap.Hours,
      time = "6 мар, 11:30",
      count = 9,
      snippet = "До конца месяца успеем",
      myShare = 0.2f
    ),
    episode(
      id = "4",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "6 мар, 14:00",
      count = 6,
      snippet = "Понял, держим в уме",
      myShare = 0.5f
    ),
    fork(id = "fork-export", gap = TimeGap.Hour),
    episode(
      id = "5",
      branchId = "export",
      gap = TimeGap.Hour,
      time = "6 мар, 14:50",
      count = 11,
      snippet = "Я про интеграцию, а не экспорт",
      myShare = 0.15f
    ),
    episode(
      id = "6",
      branchId = "terms",
      gap = TimeGap.Hours,
      time = "6 мар, 17:30",
      count = 13,
      snippet = "Ключи выдам завтра утром",
      myShare = 0.4f
    ),
    fork(id = "fork-design", gap = TimeGap.Hour),
    episode(
      id = "7",
      branchId = "design",
      gap = TimeGap.Hour,
      time = "6 мар, 18:30",
      count = 7,
      snippet = "Дизайн отдали на ревью",
      myShare = 0.33f
    ),
    episode(
      id = "8",
      branchId = TRUNK,
      gap = TimeGap.Day,
      time = "7 мар, 09:00",
      count = 6,
      snippet = "Собираемся в четверг",
      myShare = 0.6f
    ),
    merge(id = "merge-terms", gap = TimeGap.Hours),
    fork(id = "fork-budget", gap = TimeGap.Hour),
    episode(
      id = "9",
      branchId = "budget",
      gap = TimeGap.Hour,
      time = "7 мар, 11:20",
      count = 18,
      snippet = "Скинул смету, посмотри цифры",
      myShare = 0.55f
    ),
    episode(
      id = "10",
      branchId = "export",
      gap = TimeGap.Hours,
      time = "7 мар, 14:00",
      count = 3,
      snippet = "Тесты на стейджинге зелёные",
      myShare = 0.7f
    ),
    fork(id = "fork-logo", gap = TimeGap.Hours),
    episode(
      id = "11",
      branchId = "logo",
      gap = TimeGap.Hour,
      time = "7 мар, 15:50",
      count = 4,
      snippet = "Тут ещё вопрос по логотипу",
      myShare = 0.25f
    ),
    fork(id = "fork-release", gap = TimeGap.Hours),
    episode(
      id = "14",
      branchId = "release",
      gap = TimeGap.Hour,
      time = "7 мар, 18:20",
      count = 15,
      snippet = "Прод выкатили, полёт нормальный",
      myShare = 0.3f
    ),
    episode(
      id = "12",
      branchId = "design",
      gap = TimeGap.Day,
      time = "8 мар, 09:00",
      count = 9,
      snippet = "Правки внесены, смотрите",
      myShare = 0.5f
    ),
    episode(
      id = "13",
      branchId = TRUNK,
      gap = TimeGap.Hour,
      time = "8 мар, 10:30",
      count = 2,
      snippet = "Отлично, забираю",
      myShare = 0.5f
    ),
    episode(
      id = "15",
      branchId = "budget",
      gap = TimeGap.Hours,
      time = "8 мар, 16:00",
      count = 5,
      snippet = "Смета согласована",
      myShare = 0.9f,
      unreadCount = 2
    ),
    episode(
      id = "16",
      branchId = "release",
      gap = TimeGap.Day,
      time = "9 мар, 10:00",
      count = 3,
      snippet = "Метрики за неделю в таблице",
      myShare = 0.66f,
      unreadCount = 3
    ),
    episode(
      id = "17",
      branchId = TRUNK,
      gap = TimeGap.Day,
      time = "10 мар, 09:00",
      count = 2,
      snippet = "Возвращаемся к срокам",
      myShare = 0.5f
    ),
    front(id = "front"),
    episode(
      id = "18",
      branchId = "export",
      gap = TimeGap.Hours,
      time = "сегодня, 12:35",
      count = 4,
      snippet = "Слушай, а стикеры мы так и не сделали",
      unreadCount = 4
    )
  )
}

private const val TRUNK = "trunk"

private fun episode(
  id: String,
  branchId: String,
  gap: TimeGap,
  time: String,
  count: Int,
  snippet: String,
  myShare: Float = DEFAULT_MY_SHARE,
  unreadCount: Long = 0
): MockNode {
  return MockNode(
    node = GraphNode(
      id = GraphNode.Id(id),
      branchId = GraphBranch.Id(branchId),
      role = GraphNodeRole.Episode,
      gap = gap
    ),
    time = time,
    count = count,
    snippet = snippet,
    myShare = myShare,
    unreadCount = unreadCount
  )
}

private fun fork(id: String, gap: TimeGap): MockNode {
  return point(id = id, gap = gap, role = GraphNodeRole.Fork)
}

private fun merge(id: String, gap: TimeGap): MockNode {
  return point(id = id, gap = gap, role = GraphNodeRole.Merge)
}

private fun front(id: String): MockNode {
  return point(id = id, gap = TimeGap.Long, role = GraphNodeRole.Front)
}

/**
 * Точка на магистрали: ветвление, слияние или фронт.
 *
 * Содержимое эпизода ей всё равно нужно, потому что `MockNode` реализует [EpisodeContent] целиком, —
 * но на экран оно не попадает: точка рисуется кругом, а не плашкой. Уйдёт вместе со всем файлом,
 * когда граф начнёт собираться из домена.
 */
private fun point(id: String, gap: TimeGap, role: GraphNodeRole): MockNode {
  return MockNode(
    node = GraphNode(
      id = GraphNode.Id(id),
      branchId = GraphBranch.Id(TRUNK),
      role = role,
      gap = gap
    ),
    time = "",
    count = 0,
    snippet = ""
  )
}

/**
 * Временный узел демо-графа: раскладочная часть плюс то, что нарисовано внутри плашки.
 *
 * Существует ровно до появления сборки графа из веток и коммитов — тогда содержимое узла придёт из
 * домена, а не из литералов экрана.
 *
 * @param node раскладочная часть: ветка, род и пауза
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
  override val time: String,
  override val count: Int,
  override val snippet: String,
  override val myShare: Float = DEFAULT_MY_SHARE,
  override val unreadCount: Long = 0,
  override val dim: Boolean = false
) : EpisodeContent
