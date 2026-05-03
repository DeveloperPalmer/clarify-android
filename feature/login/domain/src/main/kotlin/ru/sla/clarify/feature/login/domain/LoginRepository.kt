package ru.sla.clarify.feature.login.domain

interface LoginRepository {
  suspend fun signIn()
}
