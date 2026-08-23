package ru.sla.clarify.uikit.extension

import androidx.compose.foundation.pager.PagerState
import kotlin.math.absoluteValue

/**
 * Насколько страница отстоит от текущей, в страницах.
 *
 * Дробная часть берётся из живого положения пейджера, поэтому значение меняется по ходу жеста, а не
 * скачком по его завершении — на этом и держится плавность всего, что от него зависит.
 *
 * @param index номер страницы
 * @return расстояние в страницах, ноль у текущей
 */
fun PagerState.pageOffsetOf(index: Int): Float {
  return ((currentPage - index) + currentPageOffsetFraction).absoluteValue
}
