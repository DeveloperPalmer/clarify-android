package ru.sla.clarify.uikit.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp

/**
 * Ужимает страницу тем сильнее, чем дальше она от текущей.
 *
 * Соседняя страница обязана выглядеть соседней, а не обрезанной текущей: уменьшенная, она читается
 * как «то, что рядом», и заодно показывает, что список листается. Разница по осям намеренная —
 * по вертикали заметнее, потому что горизонтальный край и так уходит за границу экрана.
 *
 * Масштаб идёт в [graphicsLayer], то есть в фазе рисования: страница не пере-измеряется, пока её
 * листают.
 *
 * @param offset расстояние до текущей страницы в страницах, см. `PagerState.pageOffset`
 * @param minScaleX ширина самой дальней страницы, долей от полной
 * @param minScaleY высота самой дальней страницы, долей от полной
 * @return модификатор с масштабом по обеим осям
 */
fun Modifier.pageScale(
  offset: Float,
  minScaleX: Float = 0.95f,
  minScaleY: Float = 0.85f
): Modifier {
  return graphicsLayer {
    val fraction = 1f - offset.coerceIn(0f, 1f)
    scaleX = lerp(start = minScaleX, stop = 1f, fraction = fraction)
    scaleY = lerp(start = minScaleY, stop = 1f, fraction = fraction)
  }
}
