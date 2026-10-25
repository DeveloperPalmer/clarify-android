package ru.sla.clarify.feature.login.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.login.data.LoginRepositoryImpl
import ru.sla.clarify.feature.login.domain.LoginRepository
import ru.sla.clarify.feature.login.domain.LoginScope

@Module
@ContributesTo(LoginScope::class)
interface LoginDataModule {
  @Binds
  fun bindLoginRepository(impl: LoginRepositoryImpl): LoginRepository
}
