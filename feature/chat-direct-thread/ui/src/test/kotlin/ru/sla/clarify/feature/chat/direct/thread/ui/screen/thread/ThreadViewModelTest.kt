package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal class ThreadViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val directThreadModel = mockk<DirectThreadModel>(relaxed = true)

  private lateinit var viewModel: ThreadViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
    viewModel = ThreadViewModel(
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
}
