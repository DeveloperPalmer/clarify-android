package ru.sla.clarify.feature.chat.group.thread.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.group.thread.data.GroupThreadRepositoryImpl
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadRepository

@Module
interface GroupThreadDataModule {
  @Binds
  fun bindGroupThreadRepository(impl: GroupThreadRepositoryImpl): GroupThreadRepository
}
