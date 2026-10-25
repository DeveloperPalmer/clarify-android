package ru.sla.clarify.feature.chat.group.thread.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.group.thread.data.GroupThreadRepositoryImpl
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadRepository
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope

@Module
@ContributesTo(GroupThreadScope::class)
interface GroupThreadDataModule {
  @Binds
  fun bindGroupThreadRepository(impl: GroupThreadRepositoryImpl): GroupThreadRepository
}
