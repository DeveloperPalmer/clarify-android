package ru.sla.clarify.feature.chat.group.thread.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.ui.screen.groupinfo.GroupInfoScreen
import ru.sla.clarify.feature.chat.group.thread.ui.screen.groupinfo.GroupInfoViewModel
import ru.sla.clarify.feature.chat.group.thread.ui.screen.main.GroupThreadScreen
import ru.sla.clarify.feature.chat.group.thread.ui.screen.main.GroupThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(GroupThreadScope::class)
object GroupThreadUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: GroupThreadViewModel): WiredComposableScreen {
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
  Main,
  GroupInfo
}
