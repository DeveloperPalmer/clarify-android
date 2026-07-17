package ru.sla.clarify.feature.chat.branch.routing.di

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams as BranchTargetParams

@SingleIn(BranchScope::class)
@ContributesSubcomponent(BranchScope::class)
interface BranchFlowComponent {
  val nodeFactory: BranchFlowNodeFactory

  @Provides
  fun provideBranchFlowComponent(): BranchFlowComponent = this

  @Provides
  @SingleIn(BranchScope::class)
  @ForScope(BranchScope::class)
  fun provideCoroutineScope(@ForScope(DirectThreadScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }

  @ContributesSubcomponent.Factory(DirectThreadScope::class)
  interface Factory {
    fun createBranchFlowComponent(params: BranchTargetParams): BranchFlowComponent
  }
}
