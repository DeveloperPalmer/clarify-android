package ru.sla.clarify.feature.chronology.ui.components.canvas

import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeAccent
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole

/**
 * Отрезок индексов, на котором ветка держит свою дорожку.
 *
 * Считается по **индексам**, а не по X, и это не экономия: X приходит из фазы измерения, а порядок
 * узлов известен уже в композиции. Где стоят плашки, отвечает [graphPlacementOf]; здесь — только на
 * какой высоте им стоять.
 *
 * **Занятость шире, чем интервал собственных узлов ветки, и в этом весь смысл функции.** §6.6 брифа
 * ставит точку слияния правее последнего сообщения — «пока merge request открыт, ветка заморожена»,
 * — поэтому горизонталь возврата тянется дальше последнего узла. Считая занятость по узлам, дорожку
 * отдали бы соседке прямо под эту линию, и возврат проехал бы сквозь её плашки.
 *
 * **Дорожку освобождает одно только слияние** (§4.2 брифа): у слитой ветки занятость кончается
 * индексом `mergedAt`, у всех прочих — концом истории, потому что незакрытая тема идёт до настоящего
 * момента. Статус здесь поэтому не читается вовсе.
 *
 * Прежде конец перечислялся по состояниям, и держалось это на «заброшенной» теме: она одна
 * освобождала дорожку, не будучи слитой. Владелец состояние отменил (журнал, итерация 39), и
 * перечисление осталось бы `when` с одной живой ветвью — то есть видом выбора там, где выбора нет.
 *
 * Начало тоже перечисляется. `forkedFrom` бывает пустым (§15 п. 10 — сообщение-развилка удалено) и
 * бывает не найден (§13 — коммит развилки ещё не догружен); тогда началом становится первый
 * собственный узел. Взять `min` по узлам молча значило бы вернуть тот же дефект зеркально: ветка с
 * развилкой на третьем узле и первым сообщением на восьмом отдала бы промежуток соседке, и уже
 * горизонталь входа проехала бы по чужим плашкам.
 *
 * @param branches ветки графа, кроме магистрали
 * @param nodeBranches ветка каждого узла, в порядке узлов
 * @param indexById индекс узла по его идентификатору
 * @return отрезок занятости для каждой ветки; ветка без единого узла и без найденной развилки
 *   в результат не попадает — занимать ей нечего
 */
internal fun branchOccupancyOf(
  branches: List<GraphBranch>,
  nodeBranches: List<GraphBranch.Id>,
  indexById: Map<GraphNode.Id, Int>
): Map<GraphBranch.Id, IntRange> {
  val ownNodes = HashMap<GraphBranch.Id, IntRange>()
  nodeBranches.forEachIndexed { index, id ->
    val known = ownNodes[id]
    ownNodes[id] = if (known == null) index..index else known.first..index
  }
  val lastIndex = nodeBranches.lastIndex
  val occupancy = HashMap<GraphBranch.Id, IntRange>()
  branches.forEach { branch ->
    val own = ownNodes[branch.id]
    val fork = branch.forkedFrom?.let { indexById[it] }
    val start = fork ?: own?.first
    if (start != null) {
      // Слияние — единственное, что освобождает дорожку по §4.2; берём его индекс, а не последний
      // узел ветки, потому что между ними лежит горизонталь возврата. Слияние, которого ещё нет в
      // наборе узлов (§13 — не догружено), держит дорожку до конца истории, как незакрытая тема.
      val end = branch.mergedAt?.let { indexById[it] } ?: lastIndex
      occupancy[branch.id] = minOf(start, own?.first ?: start)..maxOf(end, own?.last ?: end)
    }
  }
  return occupancy
}

/**
 * Номер дорожки для каждой ветки: жадная раскраска по отрезкам занятости.
 *
 * Ветки разбираются в порядке ветвления, каждая занимает ближайшую к магистрали дорожку, чьи
 * отрезки с её отрезком не пересекаются, чередуя вниз и вверх: `+1, −1, +2, −2, …`. Именно здесь
 * живёт переиспользование дорожки из §4.2: ветка, слившаяся до начала следующей, свою дорожку
 * отдаёт.
 *
 * Магистраль закреплена за нулём и в раскраске не участвует: иначе переписка без веток — самый
 * частый случай §13 — уехала бы на дорожку `+1` целиком.
 *
 * Раскраска жадная, а не оптимальная: она даёт корректную раскладку, но не наименьшее число
 * дорожек — ветка может занять ряд, который выгоднее было приберечь. Цена принята сознательно,
 * обратный ход — сортировать отрезки перед раскраской.
 *
 * Потолок §4.2 в семь дорожек здесь **не ставится**: §18 п. 6 закрыт решением «вертикальный скролл
 * полотна», и восьмая ветка получает дорожку за потолком, а не теряется.
 *
 * @param occupancy отрезки занятости, см. [branchOccupancyOf]
 * @param order ветки в порядке ветвления
 * @return номер дорожки для каждой ветки из [occupancy]
 */
internal fun laneAssignmentOf(
  occupancy: Map<GraphBranch.Id, IntRange>,
  order: List<GraphBranch.Id>
): Map<GraphBranch.Id, Int> {
  val takenByLane = HashMap<Int, MutableList<IntRange>>()
  val lanes = HashMap<GraphBranch.Id, Int>()
  order.forEach { id ->
    val span = occupancy[id]
    if (span != null) {
      var step = 1
      var lane = 1
      while (takenByLane[lane].orEmpty().any { it.overlaps(span) }) {
        step++
        // Чередование вниз и вверх: 1, −1, 2, −2 … Знак задаётся чётностью шага, величина — его
        // половиной с округлением вверх, поэтому пары дорожек заполняются от магистрали наружу.
        lane = if (step % 2 == 0) -(step / 2) else (step + 1) / 2
      }
      takenByLane.getOrPut(lane) { mutableListOf() }.add(span)
      lanes[id] = lane
    }
  }
  return lanes
}

/**
 * Цвет идентичности ветки как номер оттенка `1…6` (§7 брифа).
 *
 * Ноль оставлен магистрали, и это не педантизм: `Int.toBranchColor` отдаёт при нуле нейтральный
 * `contentTertiary`, поэтому наивное `order.mod(6)` красило бы **шестую** ветку переписки цветом
 * магистрали.
 *
 * Повтор через шесть веток неизбежен — оттенков шесть, а счёт идёт за всю жизнь переписки, — и
 * допустим: бриф просит набор из четырёх-шести.
 *
 * @param order порядковый номер ветки по времени ветвления, начиная с единицы
 * @return номер оттенка от одного до шести
 */
internal fun branchColorIndexOf(order: Int): Int {
  return 1 + (order - 1).mod(6)
}

/**
 * Пересекаются ли два отрезка занятости хотя бы одним индексом.
 *
 * @param other второй отрезок
 * @return `true`, когда дорожку делить нельзя
 */
private fun IntRange.overlaps(other: IntRange): Boolean {
  return first <= other.last && other.first <= last
}

/**
 * Дорожка каждого узла по дорожкам их веток.
 *
 * Магистраль и всё, чему дорожка не назначена, остаются на нуле: ветка без единого узла в раскраску
 * не попадает, а узла у неё и нет, — но узел, чья ветка неизвестна раскладке, обязан оказаться на
 * магистрали, а не за границей списка.
 *
 * @param nodeBranches ветка каждого узла, в порядке узлов
 * @param lanes номер дорожки каждой ветки, см. [laneAssignmentOf]
 * @return номер дорожки каждого узла, в порядке узлов
 */
internal fun nodeLanesOf(
  nodeBranches: List<GraphBranch.Id>,
  lanes: Map<GraphBranch.Id, Int>
): List<Int> {
  return nodeBranches.map { lanes[it] ?: 0 }
}

/**
 * Сколько дорожек занято по обе стороны магистрали.
 *
 * Нужна затем, чтобы потолок §4.2 можно было проверить, не пересчитывая раскраску: полотно растёт
 * на `LANE_STEP` за каждую дорожку, и восьмая ветка добавляет 104 dp высоты.
 *
 * @param lanes номера дорожек веток
 * @return число различных дорожек, включая магистраль
 */
internal fun laneCountOf(lanes: Map<GraphBranch.Id, Int>): Int {
  var below = 0
  var above = 0
  lanes.values.forEach { lane ->
    if (lane > below) below = lane
    if (-lane > above) above = -lane
  }
  return below + above + 1
}

/**
 * Цвет и направление каждого узла: чем его рисовать.
 *
 * У плашки акцент собственный, у точки ветвления и точки слияния — **чужой**: они стоят на
 * магистрали, а показывают ветку, которая от них уходит или в них возвращается. Поэтому функция и
 * существует: вывести это на месте отрисовки нельзя — узел не знает ни своей дорожки, ни чужой.
 *
 * Ветка ищется по идентификатору узла, а не по порядку, потому что от одного коммита может уйти
 * несколько веток: тогда первая из них и задаёт акцент точке, а остальные получат свои собственные
 * точки ветвления — по одной на ветку.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки графа, кроме магистрали
 * @param branchLanes номер дорожки каждой ветки, см. [laneAssignmentOf]
 * @param branchColors цвет идентичности каждой ветки
 * @return акцент каждого узла, в порядке [nodes]
 */
internal fun nodeAccentsOf(
  nodes: List<GraphNode>,
  branches: List<GraphBranch>,
  branchLanes: Map<GraphBranch.Id, Int>,
  branchColors: Map<GraphBranch.Id, Int>
): List<GraphNodeAccent> {
  val forkedAt = HashMap<GraphNode.Id, GraphBranch.Id>()
  val mergedAt = HashMap<GraphNode.Id, GraphBranch.Id>()
  branches.forEach { branch ->
    branch.forkedFrom?.let { forkedAt.putIfAbsent(it, branch.id) }
    branch.mergedAt?.let { mergedAt.putIfAbsent(it, branch.id) }
  }
  return nodes.map { node ->
    val owner = when (node.role) {
      GraphNodeRole.Fork -> forkedAt[node.id] ?: node.branchId
      GraphNodeRole.Merge -> mergedAt[node.id] ?: node.branchId
      GraphNodeRole.Episode, GraphNodeRole.Front -> node.branchId
    }
    GraphNodeAccent(
      lane = branchLanes[owner] ?: 0,
      colorIndex = branchColors[owner] ?: 0
    )
  }
}

/**
 * Дорожки и акценты одним вызовом: всё, что выводится из порядка узлов и списка веток.
 *
 * Существует затем, чтобы **композиция и измерение считали это одинаково**. Точке ветвления цвет и
 * направление нужны в composable-функции, то есть до всякого измерения, а раскладке те же дорожки
 * нужны в measure — и два независимых вызова разошлись бы на кадре, где узлы уже подменены, а
 * ветки ещё нет. Одна функция на оба места делает расхождение невозможным.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки графа, кроме магистрали, в порядке ветвления
 * @return дорожки узлов, их акценты и цвета веток
 */
internal fun graphAccentsOf(
  nodes: List<GraphNode>,
  branches: List<GraphBranch>
): List<GraphNodeAccent> {
  val branchIds = nodes.map { it.branchId }
  val indexById = nodes.withIndex().associate { (index, node) -> node.id to index }
  val branchLanes = laneAssignmentOf(
    occupancy = branchOccupancyOf(branches, branchIds, indexById),
    order = branches.map { it.id }
  )
  val branchColors = branches.associate { it.id to it.colorIndex }
  return nodeAccentsOf(nodes, branches, branchLanes, branchColors)
}

/**
 * Дорожка каждого узла по тем же данным, что и [graphAccentsOf].
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки графа, кроме магистрали, в порядке ветвления
 * @return номер дорожки каждого узла, в порядке [nodes]
 */
internal fun graphLanesOf(
  nodes: List<GraphNode>,
  branches: List<GraphBranch>
): List<Int> {
  val branchIds = nodes.map { it.branchId }
  val indexById = nodes.withIndex().associate { (index, node) -> node.id to index }
  val branchLanes = laneAssignmentOf(
    occupancy = branchOccupancyOf(branches, branchIds, indexById),
    order = branches.map { it.id }
  )
  return nodeLanesOf(branchIds, branchLanes)
}
