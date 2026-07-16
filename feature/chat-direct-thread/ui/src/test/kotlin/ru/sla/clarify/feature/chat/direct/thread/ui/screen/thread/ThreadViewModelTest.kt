package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import app.cash.turbine.test
import io.mockk.mockk
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
      assertNull(awaitItem().menuCommit)

      intents.showCommitMenu(commit)

      assertEquals(commit, awaitItem().menuCommit)
    }
  }

  @Test
  fun `when dismissCommitMenu should close the menu`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showCommitMenu(commit)
      assertEquals(commit, awaitItem().menuCommit)

      intents.dismissCommitMenu()
      assertNull(awaitItem().menuCommit)
    }
  }
}
