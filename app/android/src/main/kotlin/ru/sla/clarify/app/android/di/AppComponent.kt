package ru.sla.clarify.app.android.di

import android.content.Context
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
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
}

interface AppComponentHolder {
  val appComponent: AppComponent
}
