package ru.sla.clarify.app.routing.di

import android.app.Activity
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ActivityContext
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ForScope
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

@Module
@ContributesTo(AppFlowScope::class)
object AppFlowModule {
  @Provides
  @SingleIn(AppFlowScope::class)
  @ForScope(AppFlowScope::class)
  fun provideCoroutineScope(@ForScope(AppScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
