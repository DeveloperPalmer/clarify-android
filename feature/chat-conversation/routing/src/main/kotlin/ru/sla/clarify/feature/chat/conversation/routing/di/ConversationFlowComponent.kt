package ru.sla.clarify.feature.chat.conversation.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.direct.thread.routing.di.DirectThreadFlowComponent
import ru.sla.clarify.feature.chat.group.thread.routing.di.GroupThreadFlowComponent
import ru.sla.clarify.feature.profile.routing.di.ProfileFlowComponent

@SingleIn(ConversationScope::class)
@MergeSubcomponent(ConversationScope::class)
interface ConversationFlowComponent {
  fun nodeFactory(): ConversationFlowNodeFactory
  fun profileFlowComponent(): ProfileFlowComponent
  fun directThreadFlowComponent(): DirectThreadFlowComponent.Builder
  fun groupThreadFlowComponent(): GroupThreadFlowComponent.Builder
}

@ContributesTo(ConversationScope::class)
@Module
object ConversationFlowModule {
  @Provides
  @SingleIn(ConversationScope::class)
  @ForScope(ConversationScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
