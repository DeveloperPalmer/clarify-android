package ru.sla.clarify.feature.chat.direct.thread.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupinfo.GroupInfoScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupinfo.GroupInfoViewModel
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupthread.GroupThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupthread.GroupThreadViewModel
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadScreen
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(ThreadScope::class)
object ThreadUiModule {
  @Provides
  @WiredScreen(Screen.Thread)
  fun provideThreadScreen(model: ThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ThreadScreen(viewModel = it) }
  }

  @Provides
  @WiredScreen(Screen.GroupThread)
  fun provideGroupThreadScreen(model: GroupThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { GroupThreadScreen(viewModel = it) }
  }

  @Provides
  @WiredScreen(Screen.GroupInfo)
  fun provideGroupInfoScreen(model: GroupInfoViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { GroupInfoScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Thread,
  GroupThread,
  GroupInfo
}
