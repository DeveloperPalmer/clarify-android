package ru.sla.clarify.feature.chat.conversation.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ChatListScreen
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ChatListViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(ConversationScope::class)
interface ConversationUiModule {
  @Provides
  @WiredScreen(Screen.Main)
  fun provideMainScreen(model: ChatListViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ChatListScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Main
}
