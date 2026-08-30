package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.graphics.Color
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Цвет идентичности ветки — какая это ветка, а не что с ней происходит (§7 брифа).
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
 * Ноль оставлен магистрали: у корневой ветки идентичности нет, и линия у неё нейтральная. Оттенки
 * идут от единицы до шести, и считает их `branchColorIndexOf` — там же объяснено, почему остаток
 * берётся со сдвигом.
 *
 * @param colors палитра активной темы
 * @return нейтральный цвет для магистрали, цвет идентичности для ветки
 */
internal fun Int.toBranchColor(colors: AppColors): Color {
  return when (this) {
    0 -> colors.contentTertiary
    1 -> colors.graphLane1
    2 -> colors.graphLane2
    3 -> colors.graphLane3
    4 -> colors.graphLane4
    5 -> colors.graphLane5
    else -> colors.graphLane6
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
 * @param colors палитра активной темы
 * @return цвет по идентификатору ветки; магистраль в карте есть и красится нейтральным
 */
internal fun Graph<BasicNode>.toBranchColors(colors: AppColors): Map<Branch.Id, Color> {
  return (listOf(baseline) + branches).associate { it.id to it.colorIndex.toBranchColor(colors) }
}
