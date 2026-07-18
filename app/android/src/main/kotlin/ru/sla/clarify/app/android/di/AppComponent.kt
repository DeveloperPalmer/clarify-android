package ru.sla.clarify.app.android.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Provides
import ru.kode.plexus.core.FeatureConfigsBuilder
import ru.kode.plexus.core.FeatureConfigsManager
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.core.domain.createCoroutineScope
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.core.domain.toggle.DefaultConfig
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
  @get:Provides
  @get:ApplicationContext
  val applicationContext: Context
) {
  abstract val conversationRepository: ConversationRepository
}

@ContributesTo(AppScope::class)
interface AppModule {
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
  fun provideFeatureConfigsManager(): FeatureConfigsManager {
    return FeatureConfigsBuilder()
      .addConfig(DefaultConfig())
      .build()
  }
}

interface AppComponentHolder {
  val appComponent: AppComponent
}
