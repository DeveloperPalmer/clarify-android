package ru.sla.clarify.feature.chat.conversation.routing.di

import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.ui.di.ConversationUiModule
import ru.sla.clarify.feature.chat.direct.thread.routing.di.DirectThreadFlowComponent
import ru.sla.clarify.feature.chat.group.thread.routing.di.GroupThreadFlowComponent
import ru.sla.clarify.feature.profile.routing.di.ProfileFlowComponent

@SingleIn(ConversationScope::class)
@Subcomponent(
  modules = [
    ConversationFlowModule::class,
    ConversationUiModule::class
  ]
)
interface ConversationFlowComponent {
  fun nodeFactory(): ConversationFlowNodeFactory
  fun profileFlowComponent(): ProfileFlowComponent
  fun directThreadFlowComponent(): DirectThreadFlowComponent.Builder
  fun groupThreadFlowComponent(): GroupThreadFlowComponent.Builder
}

@Module
object ConversationFlowModule {
  @Provides
  @SingleIn(ConversationScope::class)
  @ForScope(ConversationScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
