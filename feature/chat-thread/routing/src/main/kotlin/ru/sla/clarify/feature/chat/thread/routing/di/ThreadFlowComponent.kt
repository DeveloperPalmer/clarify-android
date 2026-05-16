package ru.sla.clarify.feature.chat.thread.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Peer

@SingleIn(ThreadScope::class)
@MergeSubcomponent(ThreadScope::class)
interface ThreadFlowComponent {
  fun nodeFactory(): ThreadFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun peerId(id: Peer.Id): Builder
    fun build(): ThreadFlowComponent
  }
}
