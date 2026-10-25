package ru.sla.clarify.feature.chat.group.thread.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams

@SingleIn(GroupThreadScope::class)
@MergeSubcomponent(GroupThreadScope::class)
interface GroupThreadFlowComponent {
  fun nodeFactory(): GroupThreadFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(target: TargetParams): Builder

    fun build(): GroupThreadFlowComponent
  }
}

@ContributesTo(GroupThreadScope::class)
@Module
object GroupThreadFlowModule {
  @Provides
  @SingleIn(GroupThreadScope::class)
  @ForScope(GroupThreadScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
