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
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.component.chat.Commit
import java.time.LocalDateTime
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal class DirectThreadViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val directThreadModel = mockk<DirectThreadModel>(relaxed = true)

  private lateinit var viewModel: DirectThreadViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
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
      assertNull(awaitItem().focusedMessage)

      intents.showMessageMenu(commit)

      assertEquals(commit, awaitItem().focusedMessage)
    }
  }

  @Test
  fun `when dismissCommitMenu should close the menu`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showMessageMenu(commit)
      assertEquals(commit, awaitItem().focusedMessage)

      intents.hideMessageMenu()
      assertNull(awaitItem().focusedMessage)
    }
  }

  @Test
  fun `when confirmDeleteCommit should start deletion for payload ids`() = runTest(testDispatcher) {
    val commitIds = listOf(DomainCommit.Id("commit-1"), DomainCommit.Id("commit-2"))

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.confirmDeleteCommit(
        ViewState.DeleteCommitsParams(ids = commitIds, forEveryone = true)
      )

      verify { directThreadModel.deleteCommits.start(commitIds, true) }
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

        intents.startEditMessage(commit)

        val state = awaitItem()
        assertEquals(commit, state.editingMessage)
        assertNull(state.focusedMessage)
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

        intents.startEditMessage(commit)

        val state = awaitItem()
        assertNull(state.editingMessage)
        assertNull(state.focusedMessage)
      }
    }

  @Test
  fun `when cancelEditMessage should reset editing mode`() = runTest(testDispatcher) {
    val commit = uiMessage(id = "commit-1", text = "hello")

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.startEditMessage(commit)
      assertEquals(commit, awaitItem().editingMessage)

      intents.cancelEditMessage()
      assertNull(awaitItem().editingMessage)
    }
  }

  @Test
  fun `when submitEditMessage should start editCommit with trimmed text`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.startEditMessage(commit)
        awaitItem()

        intents.submitEditMessage("  hello edited  ")

        // Композер закрывается сразу (оптимистично), не дожидаясь ответа сервера.
        assertNull(awaitItem().editingMessage)
        verify { directThreadModel.editCommit.start(DomainCommit.Id("commit-1"), "hello edited") }
      }
    }

  @Test
  fun `when submitEditMessage with unchanged or blank text should not start editCommit`() =
    runTest(testDispatcher) {
      val commit = uiMessage(id = "commit-1", text = "hello")

      viewModel.viewStateFlow.test {
        awaitItem() // initial state

        intents.startEditMessage(commit)
        awaitItem()

        intents.submitEditMessage("  hello  ")
        intents.submitEditMessage("   ")

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

      intents.startEditMessage(editTarget)
      awaitItem()

      intents.toggleMessageSelection(selectTarget)

      val state = awaitItem()
      assertEquals(editTarget, state.editingMessage)
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

        intents.startEditMessage(commit)
        assertEquals(commit, awaitState { it.editingMessage != null }.editingMessage)

        commitsFlow.value = emptyList()

        assertNull(awaitState { it.commits.isEmpty() }.editingMessage)
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
