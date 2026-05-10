package ru.sla.clarify.feature.chat.ui.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.assisted.AssistedFactory
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
  fun provideChatThreadScreen(factory: ViewModelFactory): WiredComposableScreenFactory {
    return WiredComposableScreenFactory { peerId: String ->
      val viewModel = factory.createThreadViewModel(peerId)
      WiredComposableScreen.bind(viewModel) { ChatThreadScreen(viewModel = it) }
    }
  }
}

fun interface WiredComposableScreenFactory {
  fun create(peerId: String): WiredComposableScreen
}

@AssistedFactory
interface ViewModelFactory {
  fun createThreadViewModel(peerId: String): ChatThreadViewModel
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  ChatList,
  ChatThread
}
