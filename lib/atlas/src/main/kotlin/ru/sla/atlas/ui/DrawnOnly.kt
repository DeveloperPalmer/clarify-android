package ru.sla.atlas.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/**
 * Рисуется поверх, места в раскладке не занимает.
 *
 * Приём полотна, а не оформление. Всё, что висит рядом с узлом — гало, подпись, чип, уходящее
 * представление на время кроссфейда, — обязано не входить в его коробку: коробка узла и есть его
 * отрезок на накопительной оси, и раздутая на ширину подписи она раздвинула бы соседей по дорожке.
 *
 * Размер объявляется нулевым, а содержимое смещается на половину себя: родитель выравнивает нулевой
 * размер по центру, поэтому смещение ставит содержимое центром в центр узла. Сдвинуть его дальше —
 * дело вызывающего: `Modifier.offset(y = …).drawnOnly()` смещает уже готовый ноль.
 *
 * @return модификатор, объявляющий нулевой размер
 */
fun Modifier.drawnOnly(): Modifier {
  return layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(width = 0, height = 0) {
      placeable.place(x = -placeable.width / 2, y = -placeable.height / 2)
    }
  }
}
