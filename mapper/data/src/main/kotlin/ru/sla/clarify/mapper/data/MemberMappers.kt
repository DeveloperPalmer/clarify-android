package ru.sla.clarify.mapper.data

import ru.sla.clarify.database.entity.DirectMemberRow
import ru.sla.clarify.entity.chat.Member

fun DirectMemberRow.toDomainModel(): Member {
  return Member(
    id = id,
    displayName = displayName,
    photoUrl = photoUrl
  )
}
