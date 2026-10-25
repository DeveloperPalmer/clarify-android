package ru.sla.clarify.app.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.app.data.channel.ChannelTransport
import ru.sla.clarify.app.data.channel.EventCursorStore
import ru.sla.clarify.app.data.channel.KtorChannelTransport
import ru.sla.clarify.app.data.channel.PersistedEventCursorStore
import ru.sla.clarify.core.domain.di.scope.AppScope

@Module
@ContributesTo(AppScope::class)
interface AppDataModule {
  @Binds
  fun bindChannelTransport(impl: KtorChannelTransport): ChannelTransport

  @Binds
  fun bindEventCursorStore(impl: PersistedEventCursorStore): EventCursorStore
}
