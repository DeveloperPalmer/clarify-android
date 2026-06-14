package ru.sla.clarify.feature.chat.group.thread.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupThreadTarget

@MergeSubcomponent(GroupThreadScope::class)
@SingleIn(GroupThreadScope::class)
interface GroupThreadFlowComponent {
  fun nodeFactory(): GroupThreadFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun target(target: GroupThreadTarget): Builder
    fun build(): GroupThreadFlowComponent
  }
}
