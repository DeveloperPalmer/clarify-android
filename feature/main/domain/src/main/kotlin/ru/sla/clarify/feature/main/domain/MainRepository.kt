package ru.sla.clarify.feature.main.domain

import ru.sla.clarify.feature.main.domain.entity.UserDetails

interface MainRepository {
  suspend fun fetchUserDetails(skipCache: Boolean)
  suspend fun getUserDetails(): UserDetails
}
