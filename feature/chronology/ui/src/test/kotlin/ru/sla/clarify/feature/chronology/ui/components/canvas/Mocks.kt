package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.graphics.Color
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.CanvasMargins
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Lanes
import ru.sla.atlas.entity.Node
import ru.sla.atlas.layout.lanesOf
import ru.sla.atlas.ui.AtlasCanvasState
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

/**
 * Цвет ветки вместо палитры темы.
 *
 * Настоящий цвет приходит из `AppColors`, которой в юнит-тесте нет и заводить её незачем: геометрия
 * цвет не толкует, а только переносит из ветки в ребро и в засечку. Здесь поэтому важно одно —
 * чтобы у веток с разными оттенками цвета были разные, а у веток с одним оттенком одинаковые: на
 * этом стоят проверки «цвет повторяется каждые шесть ответвлений».
 *
 * @param colorIndex номер оттенка ветки
 * @return цвет, однозначно соответствующий номеру
 */
internal fun mockLaneColor(colorIndex: Int): Color {
  return Color(red = colorIndex * 20, green = 0, blue = 0)
}

/**
 * Цвета всех веток графа: то, что настоящему полотну отдаёт `Graph.toBranchColors`.
 *
 * @return цвет каждой ветки графа, магистраль включая
 */
internal fun Graph<Node>.mockBranchColors(): Map<Branch.Id, Color> {
  return (listOf(baseline) + branches).associate { it.id to mockLaneColor(it.colorIndex) }
}

/**
 * Дорожки и акценты графа беседы — ровно то, что считает полотно.
 *
 * Заведено затем, чтобы тест не пересказывал в каждом вызове, какой род узла за какую ветку
 * говорит: это знание фичи, и живёт оно в одном месте — [foreignBranchOf].
 *
 * @return дорожки узлов и их акценты
 */
internal fun Graph<GraphNode>.mockLanes(): Lanes {
  return lanesOf(this, mockBranchColors(), ::foreignBranchOf)
}

/**
 * Поля полотна в тесте: базовый отступ и никаких системных врезок.
 *
 * Врезки нулевые не ради краткости, а потому что окна в юнит-тесте нет вовсе — проверяется
 * арифметика камеры, а не то, как высоко на устройстве стоят часы. Отступ при этом настоящий: он
 * входит в границы содержимого, и обнулив его, тест мерил бы уже не то полотно, что показывает
 * экран.
 *
 * @return поля при плотности `1f`, где пиксель равен точке
 */
internal fun mockCanvasMargins(): CanvasMargins {
  return CanvasMargins(left = 64f, top = 64f, right = 64f, bottom = 64f)
}

/**
 * Полотно хронологии в исходном состоянии: свои уровни детализации и эпизоды на старте.
 *
 * Заведено затем, чтобы тест не повторял в каждом вызове оба параметра типа и обе величины: чем
 * полотно этой фичи отличается от любого другого, сказано в одном месте.
 *
 * @return состояние, каким его получает экран при открытии
 */
internal fun mockCanvasState(): AtlasCanvasState<GraphNode, GraphLevel> {
  return AtlasCanvasState(levels = ChronologyLevels, initialLevel = GraphLevel.Episodes)
}
