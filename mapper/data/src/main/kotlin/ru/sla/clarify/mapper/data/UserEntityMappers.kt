package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.database.entity.UserEntity

fun UserEntity.toDomainModel(): User {
  return User(
    id = id,
    email = Email(email),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
