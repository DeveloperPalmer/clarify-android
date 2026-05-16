package ru.sla.clarify.feature.login.domain

import ru.sla.clarify.feature.login.entity.AuthResult

interface LoginRepository {
  suspend fun signIn(): AuthResult
}
