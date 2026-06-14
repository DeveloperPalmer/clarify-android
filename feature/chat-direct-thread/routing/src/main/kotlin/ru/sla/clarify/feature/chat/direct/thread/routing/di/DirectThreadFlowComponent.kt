package ru.sla.clarify.feature.chat.direct.thread.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.branch.routing.di.BranchFlowComponent
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams

@SingleIn(DirectThreadScope::class)
@MergeSubcomponent(DirectThreadScope::class)
interface DirectThreadFlowComponent {
  fun nodeFactory(): DirectThreadFlowNodeFactory
  fun branchFlowComponent(): BranchFlowComponent.Builder

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun params(target: TargetParams): Builder
    fun build(): DirectThreadFlowComponent
  }
}
