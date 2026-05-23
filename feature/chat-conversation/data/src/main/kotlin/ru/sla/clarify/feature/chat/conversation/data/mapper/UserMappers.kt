package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.User as DbUser

internal fun DbUser.toDomain(): User {
  return User(
    id = UserId(id),
    email = email?.let(::Email),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
