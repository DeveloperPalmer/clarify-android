package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Branch

/**
 * @param contentLoadState состояние загрузки экрана
 * @param branches ветки переписки
 * @param debugOverlayAvailable включена ли отладочная панель фича-тоглом: от этого зависит, есть ли
 *   на экране кнопка её показа
 * @param debugOverlayVisible показана ли панель сейчас; переключается кнопкой, а панель ест место и
 *   закрывает граф, поэтому убирать её надо уметь, не выключая тогл целиком
 */
@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val branches: List<Branch> = emptyList(),
  val debugOverlayAvailable: Boolean = false,
  val debugOverlayVisible: Boolean = true
)
