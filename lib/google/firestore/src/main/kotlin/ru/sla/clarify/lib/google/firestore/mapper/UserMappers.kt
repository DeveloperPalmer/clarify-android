package ru.sla.clarify.lib.google.firestore.mapper

import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.entity.UserNM

internal fun UserNM.toDomainModel(): User {
  return User(
    id = UserId(id),
    email = Email(email),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
