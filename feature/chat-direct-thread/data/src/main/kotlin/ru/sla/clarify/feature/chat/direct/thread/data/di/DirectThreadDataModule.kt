package ru.sla.clarify.feature.chat.direct.thread.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.direct.thread.data.DirectThreadRepositoryImpl
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository

@Module
interface DirectThreadDataModule {
  @Binds
  fun bindDirectThreadRepository(impl: DirectThreadRepositoryImpl): DirectThreadRepository
}
