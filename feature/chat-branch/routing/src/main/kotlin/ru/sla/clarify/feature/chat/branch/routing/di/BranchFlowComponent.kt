package ru.sla.clarify.feature.chat.branch.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams as BranchTargetParams

@SingleIn(BranchScope::class)
@MergeSubcomponent(BranchScope::class)
interface BranchFlowComponent {
  fun nodeFactory(): BranchFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(params: BranchTargetParams): Builder

    fun build(): BranchFlowComponent
  }
}

@Module
@ContributesTo(BranchScope::class)
object BranchFlowModule {
  @Provides
  @SingleIn(BranchScope::class)
  @ForScope(BranchScope::class)
  fun provideCoroutineScope(@ForScope(DirectThreadScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
