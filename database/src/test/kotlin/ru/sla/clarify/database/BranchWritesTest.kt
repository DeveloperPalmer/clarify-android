package ru.sla.clarify.database

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

class BranchWritesTest {

  private lateinit var database: ChatDatabase

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `upsertBranch writes the branch with its merge request`() = runTest {
    insertConversation()

    database.upsertBranch(branch(mergeRequest = mergeRequest()))

    val row = database.chatBranchDao().selectById(Branch.Id("b1"))
    assertEquals("branch", row?.name)
    assertEquals("open", row?.mergeRequestStatus)
    assertEquals(setOf(UserId("alice")), row?.mergeRequestApprovedByIds)
  }

  @Test
  fun `upsertBranch without a merge request removes the stored one`() = runTest {
    insertConversation()
    database.upsertBranch(branch(mergeRequest = mergeRequest()))

    database.upsertBranch(branch(mergeRequest = null))

    assertNull(database.chatBranchDao().selectById(Branch.Id("b1"))?.mergeRequestStatus)
  }

  /**
   * Репозитории зовут запись ветки изнутри своей транзакции — вложенная транзакция не должна падать.
   */
  @Test
  fun `upsertBranch works inside an outer transaction`() = runTest {
    insertConversation()

    database.withWriteTransaction {
      database.upsertBranch(branch(mergeRequest = mergeRequest()))
    }

    assertEquals("open", database.chatBranchDao().selectById(Branch.Id("b1"))?.mergeRequestStatus)
  }

  private fun branch(mergeRequest: BranchRecord.MergeRequest?): BranchRecord {
    return BranchRecord(
      id = Branch.Id("b1"),
      conversationId = Conversation.Id("c1"),
      parentBranchId = Branch.Id("main"),
      branchedFromCommitId = Commit.Id("commit"),
      name = "branch",
      lastCommitText = null,
      lastCommitAtSeconds = 0,
      createdAtSeconds = 1,
      createdById = UserId("author"),
      mergeRequest = mergeRequest
    )
  }

  private fun mergeRequest(): BranchRecord.MergeRequest {
    return BranchRecord.MergeRequest(
      status = Branch.MergeRequest.Status.Open,
      initiatorId = UserId("author"),
      requestedAtSeconds = 10,
      approvedByIds = setOf(UserId("alice"))
    )
  }

  private suspend fun insertConversation() {
    database.useWriterConnection {
      it.executeSQL("INSERT INTO ChatConversation (id, type, lastCommitTimestamp) VALUES ('c1', 'group', 0)")
    }
  }
}
