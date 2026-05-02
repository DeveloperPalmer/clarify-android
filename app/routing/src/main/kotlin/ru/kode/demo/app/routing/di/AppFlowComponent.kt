package ru.kode.demo.app.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Subcomponent
import ru.kode.demo.core.domain.di.scope.SingleIn
import ru.kode.demo.core.ui.FlowEventSink
import ru.kode.demo.feature.main.routing.di.MainFlowComponent

@MergeSubcomponent(AppFlowScope::class)
@SingleIn(AppFlowScope::class)
interface AppFlowComponent {
  fun nodeFactory(): AppFlowNodeFactory

  fun mainFlowComponent(): MainFlowComponent

  @Subcomponent.Builder
  interface Builder {
    @BindsInstance
    fun eventSink(sink: FlowEventSink): Builder
    fun build(): AppFlowComponent
  }
}
