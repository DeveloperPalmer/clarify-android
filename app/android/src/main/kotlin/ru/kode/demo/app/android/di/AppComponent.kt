package ru.kode.demo.app.android.di

import android.content.Context
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import ru.kode.demo.app.android.Application
import ru.kode.demo.app.domain.buildconfig.BuildConfigProvider
import ru.kode.demo.app.routing.di.AppFlowComponent
import ru.kode.demo.core.domain.di.scope.AppScope
import ru.kode.demo.core.domain.di.scope.ApplicationContext
import ru.kode.demo.core.domain.di.scope.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
interface AppComponent {
  fun appFlowComponentBuilder(): AppFlowComponent.Builder

  @Component.Builder
  interface Builder {
    @BindsInstance
    fun applicationContext(@ApplicationContext context: Context): Builder
    fun build(): AppComponent
  }
}

@Module
@ContributesTo(AppScope::class)
object AppModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideBuildConfiguration(@ApplicationContext context: Context): BuildConfigProvider {
    return context as Application
  }
}

interface AppComponentHolder {
  val appComponent: AppComponent
}
