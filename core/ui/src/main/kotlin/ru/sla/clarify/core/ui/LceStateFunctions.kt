package ru.sla.clarify.core.ui

import ru.sla.clarify.core.domain.entity.LceState
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.error.ErrorMapper
import ru.sla.clarify.core.ui.error.baseErrorMappers

fun <T : Any> LceState<T>.toUiLceState(
  ignoreError: Boolean = false,
  errorMapper: ErrorMapper = ::baseErrorMappers
): ContentLoadState {
  return when (this) {
    is LceState.Content -> {
      ContentLoadState.Ready
    }
    is LceState.Loading -> {
      ContentLoadState.Loading
    }
    is LceState.Error -> if (ignoreError) {
      ContentLoadState.Ready
    } else {
      ContentLoadState.Error(
        error = errorMapper(this.value),
        refreshInProgress = false
      )
    }
  }
}
