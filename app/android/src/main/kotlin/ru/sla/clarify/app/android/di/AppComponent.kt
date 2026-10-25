package ru.sla.clarify.app.android.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import ru.kode.plexus.core.FeatureConfigsBuilder
import ru.kode.plexus.core.FeatureConfigsManager
import ru.sla.clarify.app.data.di.AppDataModule
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.app.domain.buildconfig.BuildType
import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.auth.session.data.di.AuthSessionDataModule
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.core.domain.di.scope.ForScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.toggle.DefaultConfig
import ru.sla.clarify.database.di.DatabaseModule
import ru.sla.clarify.feature.chat.conversation.data.di.ConversationDataModule
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.debug.panel.data.config.DebugConfig
import ru.sla.clarify.feature.debug.panel.routing.di.DebugPanelFlowComponent
import ru.sla.clarify.lib.google.firestore.di.FirestoreModule
import javax.inject.Provider

@SingleIn(AppScope::class)
@Component(
  modules = [
    AppModule::class,
    AppDataModule::class,
    DatabaseModule::class,
    FirestoreModule::class,
    AuthSessionDataModule::class,
    ConversationDataModule::class
  ]
)
interface AppComponent {
  fun appFlowComponentBuilder(): AppFlowComponent.Builder
  fun debugPanelFlowComponentBuilder(): DebugPanelFlowComponent.Builder

  fun conversationRepository(): ConversationRepository

  @Component.Builder
  interface Builder {
    @BindsInstance
    fun applicationContext(@ApplicationContext context: Context): Builder
    fun build(): AppComponent
  }
}

@Module
object AppModule {
  @Provides
  @SingleIn(AppScope::class)
  fun provideBuildConfiguration(@ApplicationContext context: Context): BuildConfigProvider {
    return context as BuildConfigProvider
  }

  @Provides
  @SingleIn(AppScope::class)
  @ForScope(AppScope::class)
  fun provideCoroutineScope(): CoroutineScope {
    return createCoroutineScope()
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideFeatureConfigsManager(
    debugConfig: Provider<DebugConfig>,
    buildConfigProvider: BuildConfigProvider
  ): FeatureConfigsManager {
    return FeatureConfigsBuilder()
      .apply {
        if (buildConfigProvider.buildType != BuildType.Release) {
          addConfig(debugConfig.get())
        }
      }
      .addConfig(DefaultConfig())
      .build()
  }
}

interface AppComponentHolder {
  val appComponent: AppComponent
}
