package ru.sla.clarify.uikit.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.extension.pageOffsetOf
import ru.sla.clarify.uikit.modifier.pageScale

/**
 * Горизонтальный пейджер: страница по центру, края соседних выглядывают с обеих сторон.
 *
 * Собран так, чтобы про листаемость не приходилось догадываться. Симметричные поля показывают, что
 * слева и справа что-то есть, а масштаб отделяет соседнюю страницу от текущей — без него выглянувший
 * край читается как продолжение той же карточки. Индикатор точками при этом не нужен: подсказку даёт
 * сам вид.
 *
 * Поля именно симметричные. Отступ с одной стороны выдаёт направление, которого нет: страницы
 * равноправны, и первая ничем не отличается от последней.
 *
 * @param state состояние пейджера
 * @param modifier модификатор пейджера
 * @param peek сколько соседней страницы видно с каждой стороны
 * @param pageSpacing зазор между страницами
 * @param page содержимое страницы по её номеру
 */
@Composable
fun AppPager(
  state: PagerState,
  modifier: Modifier = Modifier,
  peek: Dp = 24.dp,
  pageSpacing: Dp = 12.dp,
  page: @Composable (index: Int) -> Unit
) {
  HorizontalPager(
    state = state,
    modifier = modifier,
    contentPadding = PaddingValues(horizontal = peek),
    pageSpacing = pageSpacing
  ) { index ->
    Box(
      modifier = Modifier.pageScale(offset = state.pageOffsetOf(index)),
      content = { page(index) }
    )
  }
}
