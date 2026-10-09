package ru.sla.clarify.feature.chat.group.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.entity.GroupRow
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group

internal fun GroupRow.toDomainModel(): Group {
  return Group(
    id = id,
    name = name.orEmpty(),
    ownerId = ownerId ?: UserId(""),
    memberCount = memberCount.toInt()
  )
}
