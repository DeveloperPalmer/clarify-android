package ru.sla.clarify.feature.login.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.login.data.LoginRepositoryImpl
import ru.sla.clarify.feature.login.domain.LoginRepository

@Module
interface LoginDataModule {
  @Binds
  fun bindLoginRepository(impl: LoginRepositoryImpl): LoginRepository
}
