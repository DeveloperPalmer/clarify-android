package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.resourcerefs.strRef
import java.time.LocalDateTime
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal class DirectThreadViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val directThreadModel = mockk<DirectThreadModel>(relaxed = true)

  private val peer = Peer(
    id = Peer.Id("peer"),
    displayName = "Аня Котова",
    photoUrl = null
  )

  private lateinit var viewModel: DirectThreadViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
    // Лента собирается из коммитов и собеседника (по нему подписываются цитаты), поэтому
    // без эмиссии peer combine молчал бы и состояние не обновлялось.
    every { directThreadModel.peer } returns MutableStateFlow(peer)
    viewModel = DirectThreadViewModel(
      eventSink = eventSink,
      directThreadModel = directThreadModel,
      dispatcher = testDispatcher
    )
    intents = ViewIntents()
    viewModel.attach(intents)
  }

  @AfterEach
  fun tearDown() {
    viewModel.destroy()
  }

  @Test
  fun `when showCommitMenu should open menu for that commit`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      assertNull(awaitItem().focusedCommit)

      intents.showMessageMenu(commit)

      assertEquals(commit, awaitItem().focusedCommit)
    }
  }

  @Test
  fun `when dismissCommitMenu should close the menu`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showMessageMenu(commit)
      assertEquals(commit, awaitItem().focusedCommit)

      intents.hideMessageMenu()
      assertNull(awaitItem().focusedCommit)
    }
  }

  @Test
  fun `when confirmDeleteCommit should start deletion for payload ids`() = runTest(testDispatcher) {
    val commitIds = listOf(DomainCommit.Id("commit-1"), DomainCommit.Id("commit-2"))

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.confirmDeleteCommits(
        ViewState.DeleteCommitsParams(ids = commitIds, forEveryone = true)
      )

      verify { directThreadModel.deleteCommits.start(commitIds, true) }
    }
  }

  @Test
  fun `when startReplyMessage on peer message should enter reply mode and close menu`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello", isSelf = false)

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showMessageMenu(commit)
        awaitItem()

        intents.showReplyMessage(commit)

        val state = awaitItem()
        assertEquals(commit, state.replyingCommit)
        assertNull(state.focusedCommit)
      }
    }

  @Test
  fun `when cancelReplyMessage should reset reply mode`() = runTest(testDispatcher) {
    val commit = uiMessage(id = "commit-1", text = "hello")

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showReplyMessage(commit)
      assertEquals(commit, awaitItem().replyingCommit)

      intents.hideReplyMessage()
      assertNull(awaitItem().replyingCommit)
    }
  }

  @Test
  fun `when reply and edit modes are entered they should replace each other`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showReplyMessage(commit)
        awaitItem()

        intents.showEditMessage(commit)
        val editingState = awaitItem()
        assertEquals(commit, editingState.editingCommit)
        assertNull(editingState.replyingCommit)

        intents.showReplyMessage(commit)
        val replyingState = awaitItem()
        assertEquals(commit, replyingState.replyingCommit)
        assertNull(replyingState.editingCommit)
      }
    }

  @Test
  fun `when replyMessage should send with reply target and reset mode`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello", isSelf = false)

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showReplyMessage(commit)
        awaitItem()

        intents.replyMessage("  reply text  ")

        assertNull(awaitItem().replyingCommit)
        verify { directThreadModel.sendMessage("reply text", commit.source) }
      }
    }

  @Test
  fun `when sendMessage should send without reply target`() =
    runTest(testDispatcher) {
      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.sendMessage("  plain text  ")

        verify { directThreadModel.sendMessage("plain text", null) }
      }
    }

  @Test
  fun `when showQuotedMessage should highlight commit until it is handled`() =
    runTest(testDispatcher) {
      val targetId = DomainCommit.Id("commit-1")

      viewModel.viewStateFlow.test {
        assertNull(awaitItem().highlightedCommitId)

        intents.showQuotedMessage(targetId)
        assertEquals(targetId, awaitItem().highlightedCommitId)

        intents.clearHighlightedCommit()
        assertNull(awaitItem().highlightedCommitId)
      }
    }

  @Test
  fun `when startEditMessage on own message should enter editing mode and close menu`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showMessageMenu(commit)
        awaitItem()

        intents.showEditMessage(commit)

        val state = awaitItem()
        assertEquals(commit, state.editingCommit)
        assertNull(state.focusedCommit)
      }
    }

  @Test
  fun `when startEditMessage on peer message should not enter editing mode`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello", isSelf = false)

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showMessageMenu(commit)
        awaitItem()

        intents.showEditMessage(commit)

        val state = awaitItem()
        assertNull(state.editingCommit)
        assertNull(state.focusedCommit)
      }
    }

  @Test
  fun `when cancelEditMessage should reset editing mode`() = runTest(testDispatcher) {
    val commit = uiMessage(id = "commit-1", text = "hello")

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showEditMessage(commit)
      assertEquals(commit, awaitItem().editingCommit)

      intents.hideEditMessage()
      assertNull(awaitItem().editingCommit)
    }
  }

  @Test
  fun `when submitEditMessage should start editCommit with trimmed text`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showEditMessage(commit)
        awaitItem()

        intents.confirmEditMessage("  hello edited  ")

        // Композер закрывается сразу (оптимистично), не дожидаясь ответа сервера.
        assertNull(awaitItem().editingCommit)
        verify { directThreadModel.editCommit.start(DomainCommit.Id("commit-1"), "hello edited") }
      }
    }

  @Test
  fun `when submitEditMessage with unchanged or blank text should not start editCommit`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.showEditMessage(commit)
        awaitItem()

        intents.confirmEditMessage("  hello  ")
        intents.confirmEditMessage("   ")

        // Ссылка на таск берётся заранее: verify по цепочке засчитал бы и сам геттер editCommit,
        // который машина зовёт при построении.
        val editCommitTask = directThreadModel.editCommit
        verify(exactly = 0) {
          editCommitTask.start(any<DomainCommit.Id>(), any<String>())
        }
      }
    }

  @Test
  fun `when toggling selection should keep editing mode`() = runTest(testDispatcher) {
    val editTarget = uiMessage(id = "commit-1", text = "hello")
    val selectTarget = uiMessage(id = "commit-2", text = "world")

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showEditMessage(editTarget)
      awaitItem()

      intents.toggleSelectionMode(selectTarget)

      val state = awaitItem()
      assertEquals(editTarget, state.editingCommit)
      assertEquals(true, state.selectionEnabled)
    }
  }

  @Test
  fun `when editing target disappears from commits should reset editing mode`() =
    runTest(testDispatcher) {
      val commitsFlow = MutableStateFlow(
        listOf<DomainCommit>(domainMessage(id = "commit-1", text = "hello"))
      )
      every { directThreadModel.commits } returns commitsFlow
      recreateViewModel()

      viewModel.viewStateFlow.test {
        // viewStateFlow отдаёт последнее состояние: эмиссия коммитов могла как склеиться
        // с начальной, так и прийти отдельно — ждём состояние по условию, а не по счётчику.
        val commit = awaitState { it.commits.isNotEmpty() }.commits.first() as Commit.Message

        intents.showEditMessage(commit)
        assertEquals(commit, awaitState { it.editingCommit != null }.editingCommit)

        commitsFlow.value = emptyList()

        assertNull(awaitState { it.commits.isEmpty() }.editingCommit)
      }
    }

  @Test
  fun `when reply target disappears from commits should reset reply mode`() =
    runTest(testDispatcher) {
      val commitsFlow = MutableStateFlow(
        listOf<DomainCommit>(domainMessage(id = "commit-1", text = "hello"))
      )
      every { directThreadModel.commits } returns commitsFlow
      recreateViewModel()

      viewModel.viewStateFlow.test {
        val commit = awaitState { it.commits.isNotEmpty() }.commits.first() as Commit.Message

        intents.showReplyMessage(commit)
        assertEquals(commit, awaitState { it.replyingCommit != null }.replyingCommit)

        commitsFlow.value = emptyList()

        assertNull(awaitState { it.commits.isEmpty() }.replyingCommit)
      }
    }

  @Test
  fun `when replying to peer message quote should be signed with peer name`() =
    runTest(testDispatcher) {
      val commitsFlow = MutableStateFlow(
        listOf<DomainCommit>(
          domainMessage(id = "commit-2", text = "reply").copy(
            replyCommit = DomainCommit.Reply(
              id = DomainCommit.Id("commit-1"),
              senderId = UserId("peer"),
              isSelf = false,
              text = "hello"
            )
          )
        )
      )
      every { directThreadModel.commits } returns commitsFlow
      recreateViewModel()

      viewModel.viewStateFlow.test {
        val commit = awaitState { it.commits.isNotEmpty() }.commits.first() as Commit.Message

        assertEquals(strRef(peer.displayName), commit.replyCommit?.author)
        assertEquals("hello", commit.replyCommit?.text)
      }
    }

  private suspend fun ReceiveTurbine<ViewState>.awaitState(
    predicate: (ViewState) -> Boolean
  ): ViewState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }

  private fun recreateViewModel() {
    viewModel.destroy()
    viewModel = DirectThreadViewModel(
      eventSink = eventSink,
      directThreadModel = directThreadModel,
      dispatcher = testDispatcher
    )
    intents = ViewIntents()
    viewModel.attach(intents)
  }

  private fun uiMessage(
    id: String,
    text: String,
    isSelf: Boolean = true
  ): Commit.Message {
    return listOf<DomainCommit>(domainMessage(id, text, isSelf))
      .toUiCommits()
      .first() as Commit.Message
  }

  private fun domainMessage(
    id: String,
    text: String,
    isSelf: Boolean = true
  ): DomainCommit.Message {
    return DomainCommit.Message(
      id = DomainCommit.Id(id),
      timestamp = LocalDateTime.of(2026, 7, 19, 12, 0),
      senderId = UserId(if (isSelf) "self" else "peer"),
      text = text,
      isSelf = isSelf,
      status = DomainCommit.Status.Sent
    )
  }
}
