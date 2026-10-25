package ru.sla.clarify.feature.chat.direct.thread.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.direct.thread.data.DirectThreadRepositoryImpl
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope

@Module
@ContributesTo(DirectThreadScope::class)
interface DirectThreadDataModule {
  @Binds
  fun bindDirectThreadRepository(impl: DirectThreadRepositoryImpl): DirectThreadRepository
}
