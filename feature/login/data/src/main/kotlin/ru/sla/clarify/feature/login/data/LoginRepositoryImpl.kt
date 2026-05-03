package ru.sla.clarify.feature.login.data

import com.squareup.anvil.annotations.ContributesBinding
import ru.sla.clarify.feature.login.domain.LoginRepository
import ru.sla.clarify.feature.login.domain.LoginScope
import javax.inject.Inject

@ContributesBinding(LoginScope::class)
class LoginRepositoryImpl @Inject constructor() : LoginRepository {
  override suspend fun login() {
    TODO("Not yet implemented")
  }

  override suspend fun register() {
    TODO("Not yet implemented")
  }
}
