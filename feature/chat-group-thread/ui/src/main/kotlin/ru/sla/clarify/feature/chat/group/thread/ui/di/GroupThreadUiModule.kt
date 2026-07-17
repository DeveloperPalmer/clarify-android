package ru.sla.clarify.feature.chat.group.thread.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.ui.screen.groupinfo.GroupInfoScreen
import ru.sla.clarify.feature.chat.group.thread.ui.screen.groupinfo.GroupInfoViewModel
import ru.sla.clarify.feature.chat.group.thread.ui.screen.main.GroupThreadScreen
import ru.sla.clarify.feature.chat.group.thread.ui.screen.main.GroupThreadViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(GroupThreadScope::class)
interface GroupThreadUiModule {
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
