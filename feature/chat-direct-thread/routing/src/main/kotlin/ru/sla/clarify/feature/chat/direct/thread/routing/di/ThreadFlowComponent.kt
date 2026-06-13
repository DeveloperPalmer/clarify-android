package ru.sla.clarify.feature.chat.direct.thread.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.ThreadTarget

@SingleIn(ThreadScope::class)
@MergeSubcomponent(ThreadScope::class)
interface ThreadFlowComponent {
  fun nodeFactory(): ThreadFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun target(target: ThreadTarget): Builder
    fun build(): ThreadFlowComponent
  }
}
