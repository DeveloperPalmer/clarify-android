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
 * Демо-граф экрана: тридцать пять узлов и девять веток, покрывающих всё, что полотно умеет рисовать
 * по-разному.
 *
 * **Набор — сторож сценариев, а не набор данных.** Каждая его ветка и каждый узел отвечают за
 * состояние, которое рисуется своим кодом; убрав одно, легко не заметить, что вместе с ним с экрана
 * пропала целая ветвь поведения. Прежний набор два десятка итераций притворялся графом, не имея ни
 * одной настоящей ветки, и вскрылось это только когда мини-карта показала его в другой проекции.
 *
 * Что именно сторожит каждая ветка:
 *
 * - `terms`, слита, дорожка +1 — сплошная приглушённая линия, точка слияния, чип «Закрыта» **над**
 *   магистралью, `dim` у эпизодов и непрочитанное внутри закрытой темы;
 * - `export`, живёт, −1 — сплошная линия и узел **позже фронта**: живая тема, идущая после того, как
 *   магистраль замолчала;
 * - `design`, ждёт одобрения, +2 — неподвижный пунктир и хвост до правого края; уходит **между
 *   последним сообщением `terms` и её слиянием**;
 * - `budget`, готова к слиянию, +1 — бегущий пунктир; занимает дорожку, освободившуюся **слиянием**
 *   `terms` (§4.2);
 * - `photos`, слита, −2 — второе слияние, но с **другой** стороны магистрали: чип «Закрыта» уходит
 *   вниз;
 * - `logo`, заброшена, +3 — линия гаснет до 40 % и **хвоста не имеет**; дорожку отдаёт, не дожидаясь
 *   конца истории;
 * - `release`, живёт, −2 — занимает дорожку, освободившуюся **слиянием** `photos`, и уходит от того
 *   же коммита, что и `pricing`;
 * - `pricing`, живёт, +3 — вторая ветка **от одного коммита** с `release`; занимает дорожку,
 *   освободившуюся **заброшенностью** `logo`;
 * - `stickers`, живёт, −3 — `forkedFrom = null`: сообщение-развилка удалено (§15 п. 10), и занятость
 *   считается от первого своего узла.
 *
 * **Цвета идут по кругу, и седьмая ветка проверяет именно это.** Оттенков шесть, счёт идёт за всю
 * жизнь переписки, поэтому `release`, `pricing` и `stickers` получают `1`, `2` и `3` повторно.
 * Наивная формула `order.mod(6)` отдала бы седьмой ветке ноль — цвет магистрали, — и заметить это
 * можно только на наборе, где седьмая ветка есть.
 *
 * **Три переиспользования дорожки, а не одно.** §6.8 различает два способа освободить дорожку —
 * слияние и заброшенность, — и набор содержит оба: `budget` встаёт за слитой `terms`, `release` за
 * слитой `photos`, `pricing` за **брошенной** `logo`. Пока в наборе было только слияние, строка
 * «заброшена» в `branchOccupancyOf` не была прикрыта ничем.
 *
 * Одновременно живых веток в конце истории шесть, то есть семь дорожек вместе с магистралью, —
 * ровно потолок §4.2. За потолок набор не заходит намеренно: это отдельный сценарий,
 * см. [mockCrowdedGraph].
 *
 * Двух слияний подряд с одной стороны здесь нет, и это тоже решение. Их чипы перекрываются на 23 dp,
 * правило пустой стороны такого случая не разбирает, и вопрос ждёт владельца — вносить незакрытый
 * дефект в набор, по которому фичу смотрят глазами, значит объявить его нормой.
 *
 * @return демо-граф целиком: узлы в хронологическом порядке и ветки в порядке ветвления
 */
internal fun mockGraph(): MockGraph {
  return MockGraph(nodes = demoNodes(), branches = demoBranches())
}

/**
 * Пустая переписка — §13 брифа.
 *
 * Граф, у которого нет ни одного узла, — не краевой случай, а первое, что видит открывший новую
 * переписку. Раскладка отдаёт на нём `GraphPlacement.Empty`, диапазон камеры вырождается в точку, а
 * рёбра и засечки мини-карты не строятся вовсе; каждое из этих мест написано отдельно, и проверить
 * их можно только пустым набором.
 *
 * @return граф без узлов и без веток
 */
internal fun mockEmptyGraph(): MockGraph {
  return MockGraph(nodes = emptyList(), branches = emptyList())
}

/**
 * Одно сообщение во всей переписке — §15 п. 1 брифа.
 *
 * «Не должен выглядеть ошибкой» — требование брифа, но для камеры это ещё и вырожденный диапазон:
 * обе оси схлопываются в точку, и правило остановки инерции обязано считать это упором, а не
 * ошибкой. Фронта здесь нет намеренно — второй узел вернул бы оси протяжённость и убрал бы ровно то,
 * ради чего набор заведён.
 *
 * @return граф из единственного эпизода на магистрали
 */
internal fun mockSingleEpisodeGraph(): MockGraph {
  return MockGraph(
    nodes = listOf(
      episode(
        id = "only",
        branchId = TRUNK,
        gap = TimeGap.Hours,
        time = "сегодня, 10:04",
        count = 1,
        snippet = "Привет! Смотри, я тут подумал…",
        myShare = 1f
      )
    ),
    branches = emptyList()
  )
}

/**
 * Переписка без веток — §13 брифа, **самый частый случай**.
 *
 * «Обязан выглядеть осмысленно, а не как сломанный граф»: граф вырождается в прямую линию с
 * эпизодами. Веток нет вовсе, поэтому все узлы стоят на нулевой дорожке, засечек у мини-карты нет, а
 * из рёбер остаются одни горизонтали магистрали. Это же единственный набор, на котором видно, что
 * магистраль не привязана к константе Y: одна дорожка — и полотно обязано схлопнуться по высоте.
 *
 * @return граф из одних эпизодов магистрали и фронта
 */
internal fun mockLinearGraph(): MockGraph {
  return MockGraph(
    nodes = listOf(
      episode(
        id = "line-1",
        branchId = TRUNK,
        gap = TimeGap.Hours,
        time = "4 мар, 12:10",
        count = 6,
        snippet = "Ага, я как раз про это",
        myShare = 0.45f
      ),
      episode(
        id = "line-2",
        branchId = TRUNK,
        gap = TimeGap.Hour,
        time = "4 мар, 12:40",
        count = 2,
        snippet = "Тогда так и договоримся",
        myShare = 0.5f
      ),
      episode(
        id = "line-3",
        branchId = TRUNK,
        gap = TimeGap.Day,
        time = "5 мар, 09:20",
        count = 11,
        snippet = "Утром перечитал — всё сходится",
        myShare = 0.8f
      ),
      episode(
        id = "line-4",
        branchId = TRUNK,
        gap = TimeGap.Long,
        time = "9 мар, 18:05",
        count = 3,
        snippet = "Прости, пропал на неделю",
        myShare = 0.2f,
        unreadCount = 3
      ),
      front(id = "line-front", gap = TimeGap.Hours)
    ),
    branches = emptyList()
  )
}

/**
 * Девять одновременно живых веток — §15 п. 9 и §18 п. 6 брифа.
 *
 * Потолок §4.2 в семь дорожек раскраска **не ставит**: восьмая ветка получает дорожку за потолком, а
 * не теряется, — вопрос закрыт решением «вертикальный скролл полотна». Набор существует затем, чтобы
 * это решение было чем проверить: девять веток, ни одна из которых не сливается и не бросается,
 * занимают девять дорожек, полотно вырастает до `9 · 104 dp` по высоте, и камера обязана дать до них
 * всех доехать.
 *
 * Заодно это единственный набор, где цвет повторяется у **соседних по времени** веток: седьмая,
 * восьмая и девятая берут оттенки первой, второй и третьей, и повтор виден не в разных концах
 * истории, а рядом.
 *
 * @return граф из магистрали и девяти живых веток
 */
internal fun mockCrowdedGraph(): MockGraph {
  return MockGraph(nodes = crowdedNodes(), branches = crowdedBranches())
}

/**
 * Ветки демо-графа в порядке ветвления.
 *
 * Порядок здесь не косметика: раскраска дорожек жадная и разбирает ветки ровно в этом порядке,
 * поэтому список, отсортированный как-нибудь иначе, дал бы другую раскладку при тех же данных.
 * `stickers` стоит последней, хотя развилки у неё нет: её занятость начинается с первого
 * собственного узла, а он в наборе последний из начал.
 *
 * @return девять веток демо-графа
 */
private fun demoBranches(): List<GraphBranch> {
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
      id = GraphBranch.Id("photos"),
      colorIndex = 5,
      forkedFrom = GraphNode.Id("fork-photos"),
      mergedAt = GraphNode.Id("merge-photos"),
      status = GraphBranchStatus.Merged
    ),
    GraphBranch(
      id = GraphBranch.Id("logo"),
      colorIndex = 6,
      forkedFrom = GraphNode.Id("fork-logo"),
      mergedAt = null,
      status = GraphBranchStatus.Abandoned
    ),
    // Седьмая, восьмая и девятая берут оттенки по кругу: `1 + (order − 1) mod 6`. Ноль оставлен
    // магистрали, и наивный остаток отдал бы седьмой ветке именно его.
    GraphBranch(
      id = GraphBranch.Id("release"),
      colorIndex = 1,
      forkedFrom = GraphNode.Id("fork-release"),
      mergedAt = null,
      status = GraphBranchStatus.Alive
    ),
    // Развилка та же, что у `release`: от одного коммита уходят две ветки. Акцент точки достаётся
    // первой из них, вторая получает собственную вертикаль своего цвета в другую сторону.
    GraphBranch(
      id = GraphBranch.Id("pricing"),
      colorIndex = 2,
      forkedFrom = GraphNode.Id("fork-release"),
      mergedAt = null,
      status = GraphBranchStatus.Alive
    ),
    // Развилки нет вовсе: сообщение, от которого ветка ушла, удалено (§15 п. 10). Занятость дорожки
    // считается от первого собственного узла, ухода с магистрали не рисуется.
    GraphBranch(
      id = GraphBranch.Id("stickers"),
      colorIndex = 3,
      forkedFrom = null,
      mergedAt = null,
      status = GraphBranchStatus.Alive
    )
  )
}

/**
 * Узлы демо-графа в хронологическом порядке.
 *
 * Задаются ветками и паузами, а не координатами: положение считает геометрия полотна, дорожку —
 * раскраска по занятости, и это заодно проверяет, что ни то, ни другое не подменено литералом.
 *
 * **Порядок строго по времени, и это не косметика.** Ось X накапливается по порядку списка, поэтому
 * набор, сгруппированный по веткам, ставил бы ветку от 8 марта левее ветки от 6-го — прямое
 * нарушение §4.1 брифа. Пауза каждого узла посчитана от предыдущего **по времени**, а не назначена
 * на глаз; все пять ступеней `TimeGap` в наборе встречаются.
 *
 * **Точки ветвления и слияния — настоящие узлы списка, а не отметки на линии.** На этом держится то,
 * что вертикаль ребра не может пересечь чужую плашку: накопительная ось даёт каждому узлу
 * собственный отрезок по X, и соседи лежат либо левее, либо правее — но не под ним.
 *
 * Разбит по дням тремя функциями, потому что целиком не помещается в предел длины функции; границы
 * дней — единственное деление, которое не приходится объяснять.
 *
 * @return тридцать пять узлов демо-графа
 */
private fun demoNodes(): List<MockNode> {
  return nodesOfMarchSix() + nodesOfMarchSeven() + nodesOfLaterDays()
}

/**
 * Шестое марта: разговор расходится на три темы.
 *
 * Здесь же стоит единственное в наборе место, где ветка уходит с магистрали **между последним
 * сообщением соседки и её слиянием**: `design` ответвляется после `b-terms-3`, но до `merge-terms`.
 * Раскладка ломалась на нём дважды — считая занятость дорожки по узлам ветки, обе получили бы одну
 * дорожку, и горизонталь возврата `terms` прошла бы сквозь плашки `design`.
 *
 * @return узлы шестого марта
 */
private fun nodesOfMarchSix(): List<MockNode> {
  return listOf(
    episode(
      id = "t-1",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "6 мар, 09:40",
      count = 14,
      snippet = "Ок, давай по порядку",
      myShare = 0.38f
    ),
    fork(id = "fork-terms", gap = TimeGap.Minutes),
    episode(
      id = "terms-1",
      branchId = "terms",
      gap = TimeGap.Hour,
      time = "6 мар, 09:52",
      count = 8,
      snippet = "Сроки я вынес сюда",
      myShare = 0.62f,
      dim = true
    ),
    episode(
      id = "terms-2",
      branchId = "terms",
      gap = TimeGap.Hours,
      time = "6 мар, 11:30",
      count = 9,
      snippet = "До конца месяца успеем",
      myShare = 0.2f,
      dim = true
    ),
    episode(
      id = "t-2",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "6 мар, 14:00",
      count = 6,
      snippet = "Понял, держим в уме"
    ),
    fork(id = "fork-export", gap = TimeGap.Hour),
    episode(
      id = "export-1",
      branchId = "export",
      gap = TimeGap.Hour,
      time = "6 мар, 14:50",
      count = 11,
      snippet = "Я про интеграцию, а не экспорт",
      myShare = 0.15f
    ),
    // Непрочитанное внутри темы, которая позже будет закрыта: §6.5 требует, чтобы бейдж после
    // слияния никуда не девался, а приглушается только линия — плашка остаётся в полную силу.
    episode(
      id = "terms-3",
      branchId = "terms",
      gap = TimeGap.Hours,
      time = "6 мар, 17:30",
      count = 13,
      snippet = "Ключи выдам завтра утром",
      myShare = 0.4f,
      unreadCount = 1,
      dim = true
    ),
    fork(id = "fork-design", gap = TimeGap.Hour),
    episode(
      id = "design-1",
      branchId = "design",
      gap = TimeGap.Hour,
      time = "6 мар, 18:30",
      count = 7,
      snippet = "Дизайн отдали на ревью",
      myShare = 0.33f
    )
  )
}

/**
 * Седьмое марта: первое слияние, второе ветвление веером и брошенная тема.
 *
 * Самый плотный день набора, и плотность здесь тоже сторож. `fork-release` — единственная развилка,
 * от которой уходят **две** ветки: точка одна, вертикали две, и акцент круга достаётся первой из
 * них. Вертикаль `pricing` при этом идёт с магистрали на третью дорожку через две чужие
 * горизонтали — это единственное место набора, где на одной вертикали нужны два мостика.
 *
 * @return узлы седьмого марта
 */
private fun nodesOfMarchSeven(): List<MockNode> {
  return listOf(
    episode(
      id = "t-3",
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
      id = "budget-1",
      branchId = "budget",
      gap = TimeGap.Hour,
      time = "7 мар, 11:20",
      count = 18,
      snippet = "Скинул смету, посмотри цифры",
      myShare = 0.55f
    ),
    episode(
      id = "export-2",
      branchId = "export",
      gap = TimeGap.Hours,
      time = "7 мар, 14:00",
      count = 3,
      snippet = "Тесты на стейджинге зелёные",
      myShare = 0.7f
    ),
    fork(id = "fork-photos", gap = TimeGap.Hour),
    // Разговор в одни ворота: полоска реплик вырождается в пустую. Она же единственное, что отличает
    // такой эпизод от диалога, пока текста в узле одна строка.
    episode(
      id = "photos-1",
      branchId = "photos",
      gap = TimeGap.Minutes,
      time = "7 мар, 15:05",
      count = 1,
      snippet = "Скинула макеты",
      myShare = 0f,
      dim = true
    ),
    fork(id = "fork-logo", gap = TimeGap.Hour),
    episode(
      id = "logo-1",
      branchId = "logo",
      gap = TimeGap.Hour,
      time = "7 мар, 15:50",
      count = 4,
      snippet = "Тут ещё вопрос по логотипу",
      myShare = 0.25f
    ),
    // Двадцать четыре сообщения — потолок кластера по §5: длиннее эпизод режется принудительно.
    // Полоска реплик здесь заполнена целиком, зеркально к `photos-1`.
    episode(
      id = "photos-2",
      branchId = "photos",
      gap = TimeGap.Hour,
      time = "7 мар, 16:40",
      count = 24,
      snippet = "Забрал все, спасибо",
      myShare = 1f,
      dim = true
    ),
    merge(id = "merge-photos", gap = TimeGap.Hour),
    fork(id = "fork-release", gap = TimeGap.Hour),
    episode(
      id = "release-1",
      branchId = "release",
      gap = TimeGap.Hour,
      time = "7 мар, 18:20",
      count = 15,
      snippet = "Прод выкатили, полёт нормальный",
      myShare = 0.3f
    ),
    episode(
      id = "pricing-1",
      branchId = "pricing",
      gap = TimeGap.Minutes,
      time = "7 мар, 18:23",
      count = 2,
      snippet = "И заодно про тарифы",
      myShare = 0.5f
    )
  )
}

/**
 * Восьмое марта и дальше: живые темы, разрыв в разговоре и фронт.
 *
 * Последний узел набора принадлежит `export` и стоит **правее фронта**: живая тема, идущая после
 * того, как магистраль замолчала, — обычное состояние, на котором хвост, привязанный к фронту, уходил
 * бы в отрицательную длину.
 *
 * @return узлы восьмого марта и позже
 */
private fun nodesOfLaterDays(): List<MockNode> {
  return listOf(
    episode(
      id = "design-2",
      branchId = "design",
      gap = TimeGap.Day,
      time = "8 мар, 09:00",
      count = 9,
      snippet = "Правки внесены, смотрите",
      myShare = 0.5f
    ),
    episode(
      id = "t-4",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "8 мар, 10:30",
      count = 2,
      snippet = "Отлично, забираю"
    ),
    // Первый узел ветки без развилки: слева от него линии нет вовсе, потому что уходить неоткуда.
    episode(
      id = "stickers-1",
      branchId = "stickers",
      gap = TimeGap.Hours,
      time = "8 мар, 13:10",
      count = 5,
      snippet = "Стикеры мы так и не сделали",
      myShare = 0.45f
    ),
    episode(
      id = "budget-2",
      branchId = "budget",
      gap = TimeGap.Hours,
      time = "8 мар, 16:00",
      count = 5,
      snippet = "Смета согласована",
      myShare = 0.9f,
      unreadCount = 2
    ),
    episode(
      id = "pricing-2",
      branchId = "pricing",
      gap = TimeGap.Hours,
      time = "8 мар, 17:30",
      count = 6,
      snippet = "Тарифы посчитал, смотри таблицу",
      myShare = 0.7f
    ),
    episode(
      id = "release-2",
      branchId = "release",
      gap = TimeGap.Day,
      time = "9 мар, 10:00",
      count = 3,
      snippet = "Метрики за неделю в таблице",
      myShare = 0.66f,
      unreadCount = 3
    ),
    episode(
      id = "t-5",
      branchId = TRUNK,
      gap = TimeGap.Day,
      time = "10 мар, 09:00",
      count = 2,
      snippet = "Возвращаемся к срокам"
    ),
    // Трёхзначный счётчик: бейдж обязан упереться в «99» и не растянуть плашку.
    episode(
      id = "stickers-2",
      branchId = "stickers",
      gap = TimeGap.Hours,
      time = "10 мар, 14:20",
      count = 7,
      snippet = "Нашёл подрядчика, присылает эскизы",
      myShare = 0.5f,
      unreadCount = 128
    ),
    // Разрыв в разговоре — единственная в наборе пауза длиннее суток. Сниппет заведомо длиннее
    // фиксированной ширины узла и обязан упереться в неё эллипсисом, а не растянуть плашку.
    episode(
      id = "t-6",
      branchId = TRUNK,
      gap = TimeGap.Long,
      time = "сегодня, 09:15",
      count = 1,
      snippet = "Так, я всё-таки соберу отдельную встречу — по срокам, по смете и по тарифам сразу, " +
        "потому что порознь мы это обсуждаем уже вторую неделю"
    ),
    front(id = "front", gap = TimeGap.Hours),
    // Доля своих реплик не задана: единственный узел набора, проверяющий значение по умолчанию.
    episode(
      id = "export-3",
      branchId = "export",
      gap = TimeGap.Hours,
      time = "сегодня, 12:35",
      count = 4,
      snippet = "Интеграцию раскатили на всех",
      unreadCount = 4
    )
  )
}

/**
 * Девять веток, ни одна из которых не закрыта и не брошена.
 *
 * Занятость у всех тянется до конца истории, поэтому переиспользовать дорожку нечем — раскраска
 * обязана выдать девять разных. Цвета идут по кругу с седьмой, и здесь повтор виден рядом, а не в
 * разных концах истории.
 *
 * @return девять живых веток
 */
private fun crowdedBranches(): List<GraphBranch> {
  return (1..CROWDED_BRANCHES).map { order ->
    GraphBranch(
      id = GraphBranch.Id("crowd-$order"),
      colorIndex = 1 + (order - 1).mod(6),
      forkedFrom = GraphNode.Id("crowd-fork-$order"),
      mergedAt = null,
      status = GraphBranchStatus.Alive
    )
  }
}

/**
 * Магистраль, от которой одна за другой уходят девять веток.
 *
 * У каждой ветки ровно по одному узлу: набор существует ради высоты полотна и раскраски дорожек, а
 * не ради содержимого плашек, и лишние эпизоды только удлинили бы его по времени.
 *
 * @return узлы графа из девяти веток
 */
private fun crowdedNodes(): List<MockNode> {
  val nodes = mutableListOf(
    episode(
      id = "crowd-start",
      branchId = TRUNK,
      gap = TimeGap.Hours,
      time = "1 мар, 08:00",
      count = 3,
      snippet = "Накидал список тем",
      myShare = 0.7f
    )
  )
  for (order in 1..CROWDED_BRANCHES) {
    nodes += fork(id = "crowd-fork-$order", gap = TimeGap.Minutes)
    nodes += episode(
      id = "crowd-$order-1",
      branchId = "crowd-$order",
      gap = TimeGap.Hour,
      time = "1 мар, ${(8 + order).toString().padStart(2, '0')}:15",
      count = order,
      snippet = "Тема номер $order",
      myShare = 0.5f
    )
  }
  nodes += front(id = "crowd-front", gap = TimeGap.Hours)
  return nodes
}

private const val TRUNK = "trunk"

/** Сколько веток живёт одновременно в [mockCrowdedGraph]: на две больше потолка §4.2. */
private const val CROWDED_BRANCHES = 9

private fun episode(
  id: String,
  branchId: String,
  gap: TimeGap,
  time: String,
  count: Int,
  snippet: String,
  myShare: Float = DEFAULT_MY_SHARE,
  unreadCount: Long = 0,
  dim: Boolean = false
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
    unreadCount = unreadCount,
    dim = dim
  )
}

private fun fork(id: String, gap: TimeGap): MockNode {
  return point(id = id, gap = gap, role = GraphNodeRole.Fork)
}

private fun merge(id: String, gap: TimeGap): MockNode {
  return point(id = id, gap = gap, role = GraphNodeRole.Merge)
}

private fun front(id: String, gap: TimeGap): MockNode {
  return point(id = id, gap = gap, role = GraphNodeRole.Front)
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
 * Демо-граф целиком: узлы и ветки, которые нельзя показывать порознь.
 *
 * Держатель полотна подменяет оба списка одним вызовом, и не случайно: занятость дорожек выводится
 * из индексов узлов по идентификаторам развилки и слияния, поэтому список веток, разъехавшийся с
 * узлами хотя бы на кадр, дал бы раскраску по чужим индексам — **молча**. Пока наборы отдавались
 * двумя функциями, ничто не мешало собрать состояние из узлов одного сценария и веток другого.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки, кроме магистрали, в порядке ветвления
 */
@Immutable
internal data class MockGraph(
  val nodes: List<MockNode>,
  val branches: List<GraphBranch>
) {

  /** Раскладочная часть узлов: то, что уходит на полотно. */
  val graphNodes: List<GraphNode>
    get() = nodes.map { it.node }

  /** Содержимое плашек по идентификатору узла: то, что уходит в состояние экрана. */
  val episodeById: Map<GraphNode.Id, EpisodeContent>
    get() = nodes.associateBy { it.node.id }
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
