package ru.sla.clarify.feature.main.data.mapper

import ru.sla.clarify.feature.main.domain.entity.UserDetails

object Mappers {
  fun mapToUserDetails(
    key: Long,
    chatSignature: String
  ): UserDetails {
    return UserDetails(
      chatSignature = chatSignature
    )
  }
}
