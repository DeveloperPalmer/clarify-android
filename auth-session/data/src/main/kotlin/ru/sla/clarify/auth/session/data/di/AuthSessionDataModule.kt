package ru.sla.clarify.auth.session.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.auth.session.data.refresh.UnavailableTokenRefresher
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistenceImpl
import ru.sla.clarify.auth.session.data.storage.AuthSessionRepositoryImpl
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.auth.session.domain.TokenRefresher
import ru.sla.clarify.core.domain.di.scope.AppScope

@Module
@ContributesTo(AppScope::class)
interface AuthSessionDataModule {
  @Binds
  fun bindAuthSessionRepository(impl: AuthSessionRepositoryImpl): AuthSessionRepository

  @Binds
  fun bindAuthSessionPersistence(impl: AuthSessionPersistenceImpl): AuthSessionPersistence

  @Binds
  fun bindTokenRefresher(impl: UnavailableTokenRefresher): TokenRefresher
}
