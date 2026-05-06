package ru.sla.clarify.feature.main.data.mapper

import ru.sla.clarify.feature.main.domain.entity.UserDetails

object Mappers {
  @Suppress("UnusedParameter") // Need keep for use member reference
  fun mapToUserDetails(
    key: Long,
    chatSignature: String
  ): UserDetails {
    return UserDetails(
      chatSignature = chatSignature
    )
  }
}
