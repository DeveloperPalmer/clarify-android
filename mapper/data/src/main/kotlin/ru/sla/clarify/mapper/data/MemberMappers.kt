package ru.sla.clarify.mapper.data

import ru.sla.clarify.entity.chat.Member

fun mapToMember(
  id: Member.Id,
  displayName: String?,
  photoUrl: String?
): Member {
  return Member(
    id = id,
    displayName = displayName,
    photoUrl = photoUrl
  )
}
