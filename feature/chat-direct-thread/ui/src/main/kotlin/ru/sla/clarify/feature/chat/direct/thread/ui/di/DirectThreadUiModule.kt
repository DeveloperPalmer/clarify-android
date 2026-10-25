package ru.sla.clarify.feature.chat.direct.thread.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.DirectThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.DirectThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(DirectThreadScope::class)
object DirectThreadUiModule {
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
