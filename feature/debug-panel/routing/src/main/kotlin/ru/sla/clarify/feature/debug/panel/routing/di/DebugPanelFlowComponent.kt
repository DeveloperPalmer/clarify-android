package ru.sla.clarify.feature.debug.panel.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope

@MergeSubcomponent(DebugPanelScope::class)
@SingleIn(DebugPanelScope::class)
interface DebugPanelFlowComponent {
  fun nodeFactory(): DebugPanelFlowNodeFactory

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun eventSink(sink: FlowEventSink): Builder

    fun build(): DebugPanelFlowComponent
  }
}
