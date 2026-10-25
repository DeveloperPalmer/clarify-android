package ru.sla.clarify.feature.login.routing.di

import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.login.domain.LoginScope

@SingleIn(LoginScope::class)
@MergeSubcomponent(LoginScope::class)
interface LoginFlowComponent {
  fun nodeFactory(): LoginFlowNodeFactory
}

@Module
@ContributesTo(LoginScope::class)
object LoginFlowModule {
  @Provides
  @SingleIn(LoginScope::class)
  @ForScope(LoginScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
