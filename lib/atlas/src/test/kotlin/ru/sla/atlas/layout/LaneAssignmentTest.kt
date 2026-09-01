package ru.sla.atlas.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch

/**
 * Сторожит то, из-за чего дорожка доставалась соседке под чужую линию возврата.
 *
 * Занятость дорожки шире, чем интервал узлов ветки: §6.6 брифа ставит точку слияния правее
 * последнего сообщения, и горизонталь возврата идёт по дорожке уже после того, как ветка замолчала.
 * Проектирование дважды теряло это место — сначала считая занятость по узлам, потом выводя её конец
 * из одного лишь `mergedAt`, — поэтому тесты здесь перечисляют состояния хвоста, а не проверяют
 * «ветка слита / не слита».
 *
 * Где ветка нарисована, проверяется отдельно: здесь только на какой она высоте.
 */
class GraphBranchGeometryTest {

  @Test
  fun `a branch occupies its lane from the fork until the merge`() {
    val occupancy = branchOccupancyOf(
      branches = listOf(branch(id = "a", forkedFrom = "n2", mergedAt = "n12", nodes = 3..8)),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    assertEquals(
      2..12,
      occupancy[Branch.Id("a")],
      "занятость идёт от развилки до слияния: между последним сообщением и слиянием лежит возврат"
    )
  }

  @Test
  fun `a branch merged before another begins shares its lane`() {
    val merged = branch(id = "a", forkedFrom = "n2", mergedAt = "n8", nodes = 3..7)
    val later = branch(id = "b", forkedFrom = "n9", mergedAt = null, nodes = 10..14)
    val occupancy = branchOccupancyOf(
      branches = listOf(merged, later),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = listOf(merged.id, later.id))

    assertEquals(
      lanes[merged.id],
      lanes[later.id],
      "дорожка освобождается слиянием и переиспользуется — §4.2 брифа"
    )
  }

  @Test
  fun `a branch whose merge lands after a neighbour began keeps its lane`() {
    // Ровно тот случай, на котором проектирование сломалось: последнее сообщение ветки `a` стоит на
    // восьмом узле, слияние — на двенадцатом, а ветка `b` уходит с магистрали между ними.
    val early = branch(id = "a", forkedFrom = "n2", mergedAt = "n12", nodes = 3..8)
    val between = branch(id = "b", forkedFrom = "n9", mergedAt = null, nodes = 10..14)
    val occupancy = branchOccupancyOf(
      branches = listOf(early, between),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = listOf(early.id, between.id))

    assertNotEquals(
      lanes[early.id],
      lanes[between.id],
      "горизонталь возврата ещё идёт по дорожке, и отдать её соседке значит провести линию сквозь её плашки"
    )
  }

  @Test
  fun `branches alive at the same time never share a lane`() {
    val first = branch(id = "a", forkedFrom = "n2", mergedAt = null, nodes = 3..3)
    val second = branch(id = "b", forkedFrom = "n4", mergedAt = null, nodes = 5..5)
    val third = branch(id = "c", forkedFrom = "n6", mergedAt = null, nodes = 7..7)
    val occupancy = branchOccupancyOf(
      branches = listOf(first, second, third),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = listOf(first.id, second.id, third.id))

    assertEquals(
      3,
      lanes.values.toSet().size,
      "три живые темы обязаны стоять на трёх разных дорожках"
    )
  }

  @Test
  fun `an open merge request keeps the lane until the end of the history`() {
    val waiting = branch(id = "a", forkedFrom = "n2", mergedAt = null, nodes = 3..5)
    val later = branch(id = "b", forkedFrom = "n9", mergedAt = null, nodes = 10..14)
    val occupancy = branchOccupancyOf(
      branches = listOf(waiting, later),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = listOf(waiting.id, later.id))

    assertNotEquals(
      lanes[waiting.id],
      lanes[later.id],
      "ветка заморожена, но жива: точка слияния появится правее, и дорожка ещё занята"
    )
  }

  @Test
  fun `a branch whose fork is missing starts at its first node`() {
    val orphan = branch(id = "a", forkedFrom = "gone", mergedAt = null, nodes = 6..9)
    val occupancy = branchOccupancyOf(
      branches = listOf(orphan),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    assertEquals(
      6,
      occupancy[orphan.id]?.first,
      "удалённое сообщение-развилка не должна ронять раскладку — §15 п. 10 брифа"
    )
  }

  @Test
  fun `a branch without a single node occupies nothing`() {
    val empty = branch(id = "a", forkedFrom = null, mergedAt = null)

    val occupancy = branchOccupancyOf(
      branches = listOf(empty),
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    assertNull(occupancy[empty.id], "занимать дорожку нечем: ни развилки, ни узлов")
  }

  @Test
  fun `lanes alternate below and above the baseline`() {
    // Узлы отданы первой ветке: у остальных занятость держится развилкой и концом истории — тем
    // же отрезком, что у неё, а раскраска обязана развести все четыре.
    val branches = List(4) { index ->
      branch(id = "b$index", forkedFrom = "n2", mergedAt = null, nodes = (3..14).takeIf { index == 0 })
    }
    val occupancy = branchOccupancyOf(
      branches = branches,
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = branches.map { it.id })

    assertEquals(
      listOf(1, -1, 2, -2),
      branches.map { lanes[it.id] },
      "дорожки заполняются от магистрали наружу, чередуя вниз и вверх"
    )
  }

  @Test
  fun `the baseline always keeps lane zero`() {
    val branch = branch(id = "a", forkedFrom = "n2", mergedAt = null)
    val lanes = laneAssignmentOf(
      occupancy = mapOf(branch.id to 2..14),
      order = listOf(branch.id)
    )

    assertEquals(
      listOf(0, 1, 0),
      nodeLanesOf(listOf(BASELINE, branch.id, BASELINE), lanes),
      "магистраль в раскраске не участвует, иначе переписка без веток уехала бы на дорожку +1"
    )
  }

  @Test
  fun `an eighth branch still gets a lane beyond the ceiling`() {
    val branches = List(8) { index ->
      branch(id = "b$index", forkedFrom = "n2", mergedAt = null, nodes = (3..14).takeIf { index == 0 })
    }
    val occupancy = branchOccupancyOf(
      branches = branches,
      indexById = indexById(),
      lastIndex = LAST_INDEX
    )

    val lanes = laneAssignmentOf(occupancy, order = branches.map { it.id })

    assertEquals(8, lanes.size, "восьмая ветка получает дорожку за потолком, а не теряется")
    assertEquals(9, laneCountOf(lanes), "полотно растёт вниз, и это вертикальный скролл — §18 п. 6")
  }
}

private val BASELINE = Branch.Id("baseline")

// Пятнадцать узлов на историю: индексы, а не координаты, — раскраска дорожек считается до фазы
// измерения и о ширинах не знает.
private const val LAST_INDEX = 14

/**
 * Ветка с её составом: узлы перечисляет она сама, отрезком индексов истории.
 *
 * @param nodes отрезок собственных узлов ветки; `null` — узлов у неё нет вовсе
 */
private fun branch(
  id: String,
  forkedFrom: String?,
  mergedAt: String?,
  nodes: IntRange? = null,
  status: Branch.Status = Branch.Status.Alive
): Branch {
  return Branch(
    id = Branch.Id(id),
    nodeIds = nodes.orEmpty().map { BasicNode.Id("n$it") },
    colorIndex = 1,
    forkedFrom = forkedFrom?.let { BasicNode.Id(it) },
    mergedAt = mergedAt?.let { BasicNode.Id(it) },
    status = status
  )
}

private fun IntRange?.orEmpty(): List<Int> {
  return this?.toList().orEmpty()
}

private fun indexById(): Map<BasicNode.Id, Int> {
  return List(LAST_INDEX + 1) { index -> BasicNode.Id("n$index") to index }.toMap()
}
