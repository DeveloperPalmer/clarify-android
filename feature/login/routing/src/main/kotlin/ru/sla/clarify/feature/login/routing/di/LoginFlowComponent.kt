package ru.sla.clarify.feature.login.routing.di

import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.login.data.di.LoginDataModule
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.ui.di.LoginUiModule

@SingleIn(LoginScope::class)
@Subcomponent(
  modules = [
    LoginDataModule::class,
    LoginFlowModule::class,
    LoginUiModule::class
  ]
)
interface LoginFlowComponent {
  fun nodeFactory(): LoginFlowNodeFactory
}

@Module
object LoginFlowModule {
  @Provides
  @SingleIn(LoginScope::class)
  @ForScope(LoginScope::class)
  fun provideCoroutineScope(@ForScope(AppFlowScope::class) parent: CoroutineScope): CoroutineScope {
    return createCoroutineScope(parent)
  }
}
