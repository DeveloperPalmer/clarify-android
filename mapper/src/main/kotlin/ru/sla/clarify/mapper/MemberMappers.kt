package ru.sla.clarify.mapper

import ru.sla.clarify.entity.chat.Member

fun mapToMember(
  id: String,
  displayName: String?,
  photoUrl: String?
): Member {
  return Member(
    id = Member.Id(id),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
