package ru.sla.clarify.feature.chat.branch.domain.entity

import ru.sla.clarify.entity.chat.Commit

/** Цель редактирования уже не существует в источнике (удалена «у всех» с другого устройства). */
data class EditTargetNotFoundException(
  val commitId: Commit.Id
) : RuntimeException("edit target commit ${commitId.value} not found")
