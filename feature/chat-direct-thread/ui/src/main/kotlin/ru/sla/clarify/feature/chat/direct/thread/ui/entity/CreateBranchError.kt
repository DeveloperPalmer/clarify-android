package ru.sla.clarify.feature.chat.direct.thread.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
sealed interface CreateBranchError {
  @Immutable
  data object Empty : CreateBranchError

  @Immutable
  data object Invalid : CreateBranchError

  @Immutable
  data object AlreadyExist : CreateBranchError

  @Immutable
  data class RangeExceeded(val max: Int) : CreateBranchError
}
