package ru.sla.atlas.layout

import androidx.compose.runtime.Immutable

/**
 * Поля полотна вокруг содержимого, в пикселях.
 *
 * По сторонам они разные, и это не украшение. Полотно занимает весь экран, включая области под
 * системными барами, поэтому крайняя плашка сверху уезжала бы под часы, а снизу — под навигационную
 * полосу. Отступ у левого края такого соседа не имеет, и одинаковое поле со всех сторон означало бы
 * либо плашку под баром, либо лишнюю пустоту слева и справа.
 *
 * @param left поле слева
 * @param top поле сверху, включая строку состояния
 * @param right поле справа
 * @param bottom поле снизу, включая навигационную полосу
 */
@Immutable
data class CanvasMargins(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float
) {

  companion object {

    /** Полотно без полей: содержимое доходит до самой кромки. */
    val Zero = CanvasMargins(left = 0f, top = 0f, right = 0f, bottom = 0f)
  }
}
