package ru.sla.clarify.feature.chat.direct.thread.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.DirectThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.DirectThreadViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(DirectThreadScope::class)
interface DirectThreadUiModule {
  @Provides
  @WiredScreen(Screen.DirectThread)
  fun provideDirectThreadScreen(model: DirectThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { DirectThreadScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  DirectThread
}
