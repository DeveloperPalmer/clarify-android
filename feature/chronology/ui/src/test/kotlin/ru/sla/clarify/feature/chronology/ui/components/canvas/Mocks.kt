package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.graphics.Color
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph

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
internal fun Graph<BasicNode>.mockBranchColors(): Map<Branch.Id, Color> {
  return (listOf(baseline) + branches).associate { it.id to mockLaneColor(it.colorIndex) }
}
