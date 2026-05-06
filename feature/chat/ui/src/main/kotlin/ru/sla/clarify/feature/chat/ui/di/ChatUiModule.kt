package ru.sla.clarify.feature.chat.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.ui.screen.list.ChatListScreen
import ru.sla.clarify.feature.chat.ui.screen.list.ChatListViewModel
import ru.sla.clarify.feature.chat.ui.screen.thread.ChatThreadScreen
import ru.sla.clarify.feature.chat.ui.screen.thread.ChatThreadViewModel
import javax.inject.Qualifier

@Module
@ContributesTo(ChatScope::class)
object ChatUiModule {
  @Provides
  @WiredScreen(Screen.ChatList)
  fun provideChatListScreen(model: ChatListViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChatListScreen(viewModel = it) }
  }

  @Provides
  @WiredScreen(Screen.ChatThread)
  fun provideChatThreadScreen(model: ChatThreadViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChatThreadScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  ChatList,
  ChatThread
}
