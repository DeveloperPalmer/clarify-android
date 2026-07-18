package ru.sla.clarify.feature.chat.conversation.ui.screen.conversation

import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.kode.plexus.core.Config
import ru.kode.plexus.core.FeatureConfigsBuilder
import ru.kode.remo.JobFlow
import ru.kode.remo.QueueingStrategy
import ru.kode.remo.Task1
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.domain.ConversationModel
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent

internal class ConversationViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val conversationModel = mockk<ConversationModel>(relaxed = true)

  private val getPeerByEmailTask = mockk<Task1<Email, Peer.Id>>(relaxed = true)
  private val getPeerByEmailJobFlow = mockk<JobFlow<Peer.Id>>(relaxed = true)

  // replay = 1, чтобы результат можно было «выложить» в поток до того, как машина на него подпишется.
  private val getPeerByEmailResults = MutableSharedFlow<Result<Peer.Id, Throwable>>(replay = 1)

  private lateinit var viewModel: ConversationViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
    every { conversationModel.getPeerByEmail } returns getPeerByEmailTask
    every { conversationModel.getPeerByEmail.jobFlow } returns getPeerByEmailJobFlow
    every { conversationModel.getPeerByEmail.jobFlow.results(any()) } returns getPeerByEmailResults

    createViewModel(groupsAvailable = true)
  }

  private fun createViewModel(groupsAvailable: Boolean) {
    viewModel = ConversationViewModel(
      dispatcher = testDispatcher,
      eventSink = eventSink,
      conversationModel = conversationModel,
      featureConfigsManager = FeatureConfigsBuilder()
        .addConfig(StubConfig(mapOf(AppFeature.GroupsAvailable.key to groupsAvailable.toString())))
        .build()
    )
    intents = ViewIntents()
    viewModel.attach(intents)
  }

  private fun recreateViewModel(groupsAvailable: Boolean) {
    viewModel.destroy()
    createViewModel(groupsAvailable = groupsAvailable)
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

      verify { eventSink.sendEvent(FlowEvent.ConversationDismissed) }
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
  fun `when groups unavailable openGroupConversation should be ignored`() = runTest(testDispatcher) {
    recreateViewModel(groupsAvailable = false)
    val conversationId = Conversation.Id("conv-1")

    intents.openGroupConversation(conversationId)
    advanceUntilIdle()

    verify(exactly = 0) { eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId)) }
  }

  @Test
  fun `when groups unavailable group conversations should be hidden from list`() =
    runTest(testDispatcher) {
      every { conversationModel.conversations } returns flowOf(listOf(directConversation, groupConversation))
      recreateViewModel(groupsAvailable = false)

      viewModel.viewStateFlow.test {
        val state = expectMostRecentItem()
        assertEquals(false, state.groupsAvailable)
        assertEquals(listOf<Conversation>(directConversation), state.conversations)
      }
    }

  @Test
  fun `when groups available group conversations should be shown in list`() =
    runTest(testDispatcher) {
      every { conversationModel.conversations } returns flowOf(listOf(directConversation, groupConversation))
      recreateViewModel(groupsAvailable = true)

      viewModel.viewStateFlow.test {
        val state = expectMostRecentItem()
        assertEquals(true, state.groupsAvailable)
        assertEquals(listOf(directConversation, groupConversation), state.conversations)
      }
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

      verify(exactly = 0) { eventSink.sendEvent(FlowEvent.ConversationDismissed) }
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

private class StubConfig(private val features: Map<String, String>) : Config {
  override fun getValueSync(key: String): String? = features[key]

  override fun getValue(key: String): Flow<String?> = flowOf(features[key])

  override fun getValues(keys: List<String>): Flow<Map<String, String?>> {
    return flowOf(keys.associateWith { features[it] })
  }
}

private val directConversation = Conversation.Direct(
  id = Conversation.Id("direct-1"),
  lastCommit = null,
  lastCommitAt = null,
  lastCommitTimestamp = 0L,
  unreadCount = 0L,
  peer = Peer(
    id = Peer.Id("peer-1"),
    displayName = "Peer",
    photoUrl = null
  )
)

private val groupConversation = Conversation.Group(
  id = Conversation.Id("group-1"),
  lastCommit = null,
  lastCommitAt = null,
  lastCommitTimestamp = 0L,
  unreadCount = 0L,
  name = "Group",
  lastCommitSenderName = null
)
