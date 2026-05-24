package ru.sla.clarify.app.routing.di

import android.app.Activity
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.di.scope.ActivityContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.event.ViewEventsHostMediator
import ru.sla.clarify.feature.chat.conversation.routing.di.ConversationFlowComponent
import ru.sla.clarify.feature.login.routing.di.LoginFlowComponent

@SingleIn(AppFlowScope::class)
@MergeSubcomponent(AppFlowScope::class)
interface AppFlowComponent {
  fun nodeFactory(): AppFlowNodeFactory

  fun conversationFlowComponent(): ConversationFlowComponent
  fun loginFlowComponent(): LoginFlowComponent

  fun viewEventsHostMediator(): ViewEventsHostMediator

  @MergeSubcomponent.Builder
  interface Builder {
    @BindsInstance
    fun eventSink(sink: FlowEventSink): Builder

    @BindsInstance
    fun activity(@ActivityContext activity: Activity): Builder

    fun build(): AppFlowComponent
  }
}
