package ru.sla.clarify.feature.chat.direct.thread.ui.entity

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import ru.sla.clarify.entity.chat.Branch

@ConsistentCopyVisibility
data class CreateBranch private constructor(val name: String) {
  companion object {
    operator fun invoke(
      name: String,
      branches: List<Branch>
    ): EitherNel<CreateBranchError, CreateBranch> = either {
      val trimmedValue = name.trim()
      val branchNames = branches.map { it.name }
      zipOrAccumulate(
        {
          ensure(trimmedValue.isNotBlank()) { CreateBranchError.Empty }
        },
        {
          ensure(trimmedValue.length <= MAX_LENGTH) { CreateBranchError.RangeExceeded(MAX_LENGTH) }
        },
        {
          ensure(allowedPattern.matches(trimmedValue)) { CreateBranchError.Invalid }
        },
        {
          ensure(!branchNames.contains(trimmedValue)) { CreateBranchError.AlreadyExist }
        }
      ) { _, _, _, _ ->
        CreateBranch(name = trimmedValue)
      }
    }
  }
}

private val allowedPattern = Regex("^[a-zA-Z0-9_-]+$")
private const val MAX_LENGTH = 40
