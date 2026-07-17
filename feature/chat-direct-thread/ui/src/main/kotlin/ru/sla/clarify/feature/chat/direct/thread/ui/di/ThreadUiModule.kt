package ru.sla.clarify.feature.chat.direct.thread.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(DirectThreadScope::class)
interface ThreadUiModule {
  @Provides
  @WiredScreen(Screen.Thread)
  fun provideThreadScreen(model: ThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ThreadScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Thread
}
