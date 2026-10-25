package ru.sla.clarify.app.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.app.data.channel.ChannelTransport
import ru.sla.clarify.app.data.channel.EventCursorStore
import ru.sla.clarify.app.data.channel.KtorChannelTransport
import ru.sla.clarify.app.data.channel.PersistedEventCursorStore

@Module
interface AppDataModule {
  @Binds
  fun bindChannelTransport(impl: KtorChannelTransport): ChannelTransport

  @Binds
  fun bindEventCursorStore(impl: PersistedEventCursorStore): EventCursorStore
}
