package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.remo.JobFlow
import ru.kode.remo.QueueingStrategy
import ru.kode.remo.Task1
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.domain.ConversationModel
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent

internal class ChatListViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val conversationModel = mockk<ConversationModel>(relaxed = true)

  private val getPeerByEmailTask = mockk<Task1<Email, Peer.Id>>(relaxed = true)
  private val getPeerByEmailJobFlow = mockk<JobFlow<Peer.Id>>(relaxed = true)

  // replay = 1, чтобы результат можно было «выложить» в поток до того, как машина на него подпишется.
  private val getPeerByEmailResults = MutableSharedFlow<Result<Peer.Id, Throwable>>(replay = 1)

  private lateinit var viewModel: ChatListViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
    every { conversationModel.getPeerByEmail } returns getPeerByEmailTask
    every { conversationModel.getPeerByEmail.jobFlow } returns getPeerByEmailJobFlow
    every { conversationModel.getPeerByEmail.jobFlow.results(any()) } returns getPeerByEmailResults

    viewModel = ChatListViewModel(
      dispatcher = testDispatcher,
      eventSink = eventSink,
      conversationModel = conversationModel
    )
    intents = ViewIntents()
    viewModel.attach(intents)
  }

  @AfterEach
  fun tearDown() {
    viewModel.destroy()
  }

  @Test
  fun `when openProfile should request profile`() = runTest(testDispatcher) {
    intents.openProfile()
    advanceUntilIdle()

    verify { eventSink.sendEvent(FlowEvent.ProfileRequested) }
  }

  @Test
  fun `when navigateBack and not in edit mode should dismiss chat list`() =
    runTest(testDispatcher) {
      intents.navigateBack()
      advanceUntilIdle()

      verify { eventSink.sendEvent(FlowEvent.ChatListDismissed) }
    }

  @Test
  fun `when openDirectConversation should request direct conversation`() = runTest(testDispatcher) {
    val peerId = Peer.Id("peer-1")

    intents.openDirectConversation(peerId)
    advanceUntilIdle()

    verify { eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId)) }
  }

  @Test
  fun `when openGroupConversation should request group conversation`() = runTest(testDispatcher) {
    val conversationId = Conversation.Id("conv-1")

    intents.openGroupConversation(conversationId)
    advanceUntilIdle()

    verify { eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId)) }
  }

  @Test
  fun `when changeEmailQuery should update query and clear error`() = runTest(testDispatcher) {
    viewModel.viewStateFlow.test {
      assertEquals("", awaitItem().directEmailQuery.text)

      intents.changeEmailQuery(TextFieldValue("peer@mail.com"))

      val state = awaitItem()
      assertEquals("peer@mail.com", state.directEmailQuery.text)
      assertNull(state.directEmailError)
    }
  }

  @Test
  fun `when conversation long pressed should enter edit mode and select it`() =
    runTest(testDispatcher) {
      val conversationId = Conversation.Id("conv-1")

      viewModel.viewStateFlow.test {
        assertEquals(false, awaitItem().editModeEnabled)

        intents.handleConversationLongPress(conversationId)

        val state = awaitItem()
        assertEquals(true, state.editModeEnabled)
        assertEquals(listOf(conversationId), state.selectedConversationsIds)
      }
    }

  @Test
  fun `when navigateBack in edit mode should exit edit mode without dismiss`() =
    runTest(testDispatcher) {
      val conversationId = Conversation.Id("conv-1")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.handleConversationLongPress(conversationId)
        assertEquals(true, awaitItem().editModeEnabled)

        intents.navigateBack()
        val state = awaitItem()
        assertEquals(false, state.editModeEnabled)
        assertEquals(emptyList<Conversation.Id>(), state.selectedConversationsIds)
      }

      verify(exactly = 0) { eventSink.sendEvent(FlowEvent.ChatListDismissed) }
    }

  @Test
  fun `when confirmDeleteConversation should start delete on model with selected ids`() =
    runTest(testDispatcher) {
      val first = Conversation.Id("conv-1")
      val second = Conversation.Id("conv-2")

      intents.handleConversationLongPress(first)
      intents.handleConversationLongPress(second)
      intents.confirmDeleteConversation()
      advanceUntilIdle()

      verify { conversationModel.deleteConversations.start(listOf(first, second)) }
    }

  @Test
  fun `when validateDirectEmail with valid email should look up peer`() = runTest(testDispatcher) {
    intents.changeEmailQuery(TextFieldValue("peer@mail.com"))
    advanceUntilIdle()

    intents.validateDirectEmail()
    advanceUntilIdle()

    verify {
      conversationModel.getPeerByEmail.start(
        argument = Email("peer@mail.com"),
        scheduled = any(),
        queueingStrategy = QueueingStrategy.SkipNew
      )
    }
  }

  @Test
  fun `when peer found and direct conversation confirmed should request direct conversation`() =
    runTest(testDispatcher) {
      val peerId = Peer.Id("peer-1")
      getPeerByEmailResults.emit(Ok(peerId))

      intents.confirmCreateDirectConversation()
      advanceUntilIdle()

      verify { eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId)) }
    }
}
