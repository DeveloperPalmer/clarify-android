package ru.sla.clarify.feature.chat.conversation.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.feature.chat.conversation.data.ConversationRepositoryImpl
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository

@Module
@ContributesTo(AppScope::class)
interface ConversationDataModule {
  @Binds
  fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
}
