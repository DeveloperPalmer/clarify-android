package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId

fun mapToUser(
  id: String,
  email: String,
  displayName: String,
  photoUrl: String?
): User {
  return User(
    id = UserId(id),
    email = Email(email),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
