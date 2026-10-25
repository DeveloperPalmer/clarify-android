package ru.sla.clarify.feature.chat.conversation.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.conversation.data.ConversationRepositoryImpl
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository

@Module
interface ConversationDataModule {
  @Binds
  fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
}
