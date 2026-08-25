package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.ui.graphics.Color
import ru.sla.clarify.uikit.theme.AppColors

/**
 * Цвет идентичности дорожки — какая это ветка, а не что с ней происходит (§7 брифа).
 *
 * Палитра приходит параметром, а не читается из `AppTheme`: цвет нужен в фазе рисования, где
 * композиции уже нет, — и это ровно тот случай, о котором правило мапперов говорит «контекст,
 * которого нет в исходном типе, передаётся параметром».
 *
 * Остаток от деления взят с приведением к неотрицательному (`mod`, а не `rem`) и берётся от самого
 * номера дорожки, а не от его модуля. Свойство, ради которого выбрана именно эта формула: **соседние
 * дорожки никогда не получают один цвет**, потому что их номера различаются на единицу. Повтор через
 * шесть дорожек неизбежен — в демо-наборе их четырнадцать, а оттенков идентичности шесть — и
 * допустим: бриф просит набор из четырёх-шести.
 *
 * @param colors палитра активной темы
 * @return нейтральный цвет для магистрали, цвет идентичности для ветки
 */
internal fun Int.toLaneColor(colors: AppColors): Color {
  if (this == 0) {
    return colors.contentTertiary
  }
  return when (this.mod(6)) {
    0 -> colors.graphLane6
    1 -> colors.graphLane1
    2 -> colors.graphLane2
    3 -> colors.graphLane3
    4 -> colors.graphLane4
    else -> colors.graphLane5
  }
}
