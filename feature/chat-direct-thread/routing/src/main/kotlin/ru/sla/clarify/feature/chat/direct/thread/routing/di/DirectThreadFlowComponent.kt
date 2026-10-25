package ru.sla.clarify.feature.chat.direct.thread.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.branch.routing.di.BranchFlowComponent
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.chronology.routing.di.ChronologyFlowComponent

@SingleIn(DirectThreadScope::class)
@MergeSubcomponent(DirectThreadScope::class)
interface DirectThreadFlowComponent {
  fun nodeFactory(): DirectThreadFlowNodeFactory
  fun branchFlowComponent(): BranchFlowComponent.Builder
  fun chronologyFlowComponent(): ChronologyFlowComponent.Builder

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(target: TargetParams): Builder

    fun build(): DirectThreadFlowComponent
  }
}

@Module
@ContributesTo(DirectThreadScope::class)
object DirectThreadFlowModule {
  @Provides
  @SingleIn(DirectThreadScope::class)
  @ForScope(DirectThreadScope::class)
  fun provideCoroutineScope(@ForScope(ConversationScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
