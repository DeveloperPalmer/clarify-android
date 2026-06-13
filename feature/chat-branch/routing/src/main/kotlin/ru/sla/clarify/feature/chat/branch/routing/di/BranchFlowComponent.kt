package ru.sla.clarify.feature.chat.branch.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.Branch

@SingleIn(BranchScope::class)
@MergeSubcomponent(BranchScope::class)
interface BranchFlowComponent {
  fun nodeFactory(): BranchFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun id(id: Branch.Id): Builder
    fun build(): BranchFlowComponent
  }
}
