package ru.sla.clarify.feature.chat.group.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.entity.GroupMemberRow
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember

/**
 * Владельца строка не знает — он берётся из группы, поэтому `isOwner` выставляет вызывающий.
 */
internal fun GroupMemberRow.toDomainModel(currentUserId: UserId?): GroupMember {
  return GroupMember(
    id = UserId(id.value),
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    isOwner = false,
    isMe = id.value == currentUserId?.value
  )
}
