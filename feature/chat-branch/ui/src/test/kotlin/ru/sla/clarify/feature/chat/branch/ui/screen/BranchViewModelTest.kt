package ru.sla.clarify.feature.chat.branch.ui.screen

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chat.branch.domain.BranchModel
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal class BranchViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val eventSink = mockk<FlowEventSink>(relaxed = true)
  private val branchModel = mockk<BranchModel>(relaxed = true)
  private val params = TargetParams(branchId = Branch.Id("branch-1"))

  private lateinit var viewModel: BranchViewModel
  private lateinit var intents: ViewIntents

  @BeforeEach
  fun setup() {
    viewModel = BranchViewModel(
      params = params,
      eventSink = eventSink,
      branchModel = branchModel,
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
  fun `when showMessageMenu should open menu for that commit`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      assertNull(awaitItem().focusedMessage)

      intents.showMessageMenu(commit)

      assertEquals(commit, awaitItem().focusedMessage)
    }
  }

  @Test
  fun `when hideMessageMenu should close the menu`() = runTest(testDispatcher) {
    val commit = mockk<Commit.Message>(relaxed = true)

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.showMessageMenu(commit)
      assertEquals(commit, awaitItem().focusedMessage)

      intents.hideMessageMenu()
      assertNull(awaitItem().focusedMessage)
    }
  }

  private fun commitWithId(id: DomainCommit.Id): Commit.Message {
    val source = mockk<DomainCommit.Message>(relaxed = true)
    every { source.id } returns id
    val commit = mockk<Commit.Message>(relaxed = true)
    every { commit.source } returns source
    return commit
  }

  @Test
  fun `when toggleMessageSelection should enable edit mode and track the commit`() =
    runTest(testDispatcher) {
      val commitId = DomainCommit.Id("commit-1")
      val commit = commitWithId(commitId)

      viewModel.viewStateFlow.test {
        val initial = awaitItem()
        assertFalse(initial.editModeEnabled)

        intents.toggleMessageSelection(commit)

        val selected = awaitItem()
        assertTrue(selected.editModeEnabled)
        assertEquals(listOf(commitId), selected.selectedCommitIds)
      }
    }

  @Test
  fun `when disableEditMode should clear the selection`() = runTest(testDispatcher) {
    val commitId = DomainCommit.Id("commit-1")
    val commit = commitWithId(commitId)

    viewModel.viewStateFlow.test {
      awaitItem() // initial state

      intents.toggleMessageSelection(commit)
      assertTrue(awaitItem().editModeEnabled)

      intents.disableEditMode()
      val cleared = awaitItem()
      assertFalse(cleared.editModeEnabled)
      assertTrue(cleared.selectedCommitIds.isEmpty())
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

      verify { branchModel.deleteCommits.start(commitIds, true) }
    }
  }
}
