package ru.sla.atlas.ui

import androidx.compose.ui.graphics.Color
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.CanvasMargins
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Lanes
import ru.sla.atlas.entity.MockNode
import ru.sla.atlas.entity.mockBranch
import ru.sla.atlas.entity.mockBranchColors
import ru.sla.atlas.entity.mockNode
import ru.sla.atlas.layout.lanesOf
import ru.sla.atlas.lod.MockLevel
import ru.sla.atlas.lod.MockLevels

/**
 * Полотно в исходном состоянии: лестница из двух уровней, подробный на старте.
 *
 * @return состояние, каким его получает вызывающий при открытии
 */
internal fun mockCanvasState(): AtlasCanvasState<MockNode, MockLevel> {
  return AtlasCanvasState(levels = MockLevels, initialLevel = MockLevel.Fine)
}

/**
 * Цепочка узлов на одной магистрали: история, у которой нет ни одной ветки.
 *
 * Самый частый граф и самый удобный для камеры: дорожка одна, и всё, что проверяется, — движение по
 * оси времени.
 *
 * @param count сколько узлов в графе
 * @return граф из [count] узлов, все — магистральные
 */
internal fun mockBaselineGraph(count: Int): Graph<MockNode> {
  val nodes = List(count) { index -> mockNode("n$index") }
  return Graph(
    nodes = nodes,
    baseline = mockBranch(id = "baseline", nodes = nodes.map { it.id.value }, colorIndex = 0),
    branches = emptyList()
  )
}

/**
 * Дорожки и акценты графа, у которого нет узлов особого рода.
 *
 * За какую ветку говорит узел, здесь спрашивать не у кого: у мока родов нет, и каждый узел говорит
 * за свою собственную ветку.
 *
 * @return дорожки узлов и их акценты
 */
internal fun Graph<MockNode>.mockLanes(): Lanes {
  return lanesOf(this, mockBranchColors()) { _, own -> own }
}

/**
 * Цвета веток графа мока.
 *
 * @return цвет каждой ветки, магистраль включая
 */
internal fun Graph<MockNode>.mockColors(): Map<Branch.Id, Color> {
  return mockBranchColors()
}

/**
 * Поля полотна в тесте: базовый отступ и никаких системных врезок.
 *
 * Врезки нулевые не ради краткости, а потому что окна в юнит-тесте нет вовсе — проверяется
 * арифметика камеры. Отступ при этом настоящий: он входит в границы содержимого, и обнулив его,
 * тест мерил бы уже не то полотно, что показывает экран.
 *
 * @return поля при плотности `1f`, где пиксель равен точке
 */
internal fun mockCanvasMargins(): CanvasMargins {
  return CanvasMargins(left = 64f, top = 64f, right = 64f, bottom = 64f)
}
