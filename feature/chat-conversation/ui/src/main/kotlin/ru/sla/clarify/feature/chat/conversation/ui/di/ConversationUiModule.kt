package ru.sla.clarify.feature.chat.conversation.ui.di

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Qualifier
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.ui.screen.conversation.ConversationScreen
import ru.sla.clarify.feature.chat.conversation.ui.screen.conversation.ConversationViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(ConversationScope::class)
interface ConversationUiModule {
  @Provides
  @WiredScreen(Screen.Conversation)
  fun provideConversationScreen(model: ConversationViewModel): WiredComposableScreen {
    return WiredComposableScreen.bind(model) { ConversationScreen(viewModel = it) }
  }
}

@Qualifier
annotation class WiredScreen(val screen: Screen)

enum class Screen {
  Conversation
}
