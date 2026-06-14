package ru.sla.clarify.feature.chat.direct.thread.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(DirectThreadScope::class)
object ThreadUiModule {
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
