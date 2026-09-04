package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.graphics.Color
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Node
import ru.sla.clarify.feature.chronology.ui.entity.BranchColor
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Оттенок идентичности — в цвет активной темы (§7 брифа).
 *
 * **Цвет принадлежит ветке, а не дорожке.** Раньше он выводился из номера дорожки, и это молча
 * ломалось ровно там, где §4.2 брифа обещает главное: дорожка освобождается после слияния и
 * переиспользуется, поэтому две несвязанные темы, вставшие на неё по очереди, получали один
 * оттенок — «цвет = идентичность» превращалось в «цвет = номер ряда».
 *
 * Палитра приходит параметром, а не читается из `AppTheme`: цвет нужен в фазе рисования, где
 * композиции уже нет, — и это ровно тот случай, о котором правило мапперов говорит «контекст,
 * которого нет в исходном типе, передаётся параметром».
 *
 * Ветка без оттенка — это магистраль: у корневой ветки идентичности нет, и линия у неё нейтральная.
 * Хвоста `else` здесь нет намеренно: седьмое значение [BranchColor] обязано сломать компиляцию
 * здесь, а не тихо покраситься шестым цветом.
 *
 * @param colors палитра активной темы
 * @return нейтральный цвет для ветки без оттенка, цвет идентичности для остальных
 */
internal fun BranchColor?.toColor(colors: AppColors): Color {
  return when (this) {
    null -> colors.contentTertiary
    BranchColor.First -> colors.graphLane1
    BranchColor.Second -> colors.graphLane2
    BranchColor.Third -> colors.graphLane3
    BranchColor.Fourth -> colors.graphLane4
    BranchColor.Fifth -> colors.graphLane5
    BranchColor.Sixth -> colors.graphLane6
  }
}

/**
 * Цвет каждой ветки графа, включая магистраль.
 *
 * Считается один раз на граф и палитру, а не на каждое ребро и каждый узел: цвет нужен раскладке в
 * трёх местах — акценты узлов, рёбра, засечки мини-карты, — и палитру пришлось бы протаскивать во
 * все три. Отсюда геометрия получает готовые цвета и о теме не знает вовсе: она спрашивает, каким
 * цветом ветка, а не светлая сейчас тема или тёмная.
 *
 * Ключи берутся у графа, а не у карты оттенков: раскладка спросит цвет каждой ветки графа, и ветка,
 * которой в карте не нашлось, красится нейтральным, а не роняет полотно.
 *
 * @param branchColors оттенок идентичности каждой ветки; магистрали в карте нет
 * @param colors палитра активной темы
 * @return цвет по идентификатору ветки; магистраль в карте есть и красится нейтральным
 */
internal fun Graph<Node>.toBranchColors(
  branchColors: Map<Branch.Id, BranchColor>,
  colors: AppColors
): Map<Branch.Id, Color> {
  return (listOf(baseline) + branches).associate { it.id to branchColors[it.id].toColor(colors) }
}
