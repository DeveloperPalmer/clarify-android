package ru.sla.clarify.database.dao

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.BranchRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

class ChatBranchDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: ChatBranchDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.chatBranchDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `observeIds returns branches of the conversation only`() = runTest {
    insertConversation("c1")
    insertConversation("c2")
    insertBranch("b1", "c1", createdAt = 1)
    insertBranch("b2", "c1", createdAt = 2)
    insertBranch("b3", "c2", createdAt = 3)

    dao.observeIds(Conversation.Id("c1")).test {
      assertEquals(setOf(Branch.Id("b1"), Branch.Id("b2")), awaitItem().toSet())
    }
  }

  /**
   * Ветка без запроса слияния отдаёт пустыми все поля слияния сразу — на этом инварианте
   * маппер решает, есть ли запрос.
   */
  @Test
  fun `observeByConversation orders by creation and joins the merge request`() = runTest {
    insertConversation("c1")
    insertBranch("late", "c1", createdAt = 2)
    insertBranch("early", "c1", createdAt = 1)
    insertMergeRequest("late")

    dao.observeByConversation(Conversation.Id("c1")).test {
      assertEquals(
        listOf(branchRow("early", createdAt = 1), mergedBranchRow("late", createdAt = 2)),
        awaitItem()
      )
    }
  }

  @Test
  fun `observeById and selectById return the same row`() = runTest {
    insertConversation("c1")
    insertBranch("b1", "c1", createdAt = 1)
    insertMergeRequest("b1")

    assertEquals(mergedBranchRow("b1", createdAt = 1), dao.selectById(Branch.Id("b1")))
    dao.observeById(Branch.Id("b1")).test {
      assertEquals(mergedBranchRow("b1", createdAt = 1), awaitItem())
    }
  }

  @Test
  fun `selectById returns null for unknown branch`() = runTest {
    assertNull(dao.selectById(Branch.Id("missing")))
  }

  /**
   * Повторная запись ветки обновляет её поля, но не трогает локальный счётчик непрочитанного.
   */
  @Test
  fun `upsert updates the row and keeps unreadCount`() = runTest {
    insertConversation("c1")
    upsertBranch(name = "old")
    execute("UPDATE ChatBranch SET unreadCount = 3 WHERE id = 'b1'")

    upsertBranch(name = "new")

    val branch = dao.selectById(Branch.Id("b1"))
    assertEquals("new", branch?.name)
    assertEquals(3L, branch?.unreadCount)
  }

  @Test
  fun `upsert of a new branch starts with zero unread`() = runTest {
    insertConversation("c1")
    upsertBranch(name = "new")

    assertEquals(0L, dao.selectById(Branch.Id("b1"))?.unreadCount)
  }

  @Test
  fun `updateUnreadCount changes only the counter of the given branch`() = runTest {
    insertConversation("c1")
    insertBranch("b1", "c1", createdAt = 1)
    insertBranch("b2", "c1", createdAt = 2)

    dao.updateUnreadCount(Branch.Id("b1"), 7)

    assertEquals(branchRow("b1", createdAt = 1).copy(unreadCount = 7), dao.selectById(Branch.Id("b1")))
    assertEquals(branchRow("b2", createdAt = 2), dao.selectById(Branch.Id("b2")))
  }

  @Test
  fun `delete removes only the given branch`() = runTest {
    insertConversation("c1")
    insertBranch("b1", "c1", createdAt = 1)
    insertBranch("b2", "c1", createdAt = 2)

    dao.delete(Branch.Id("b1"))

    assertNull(dao.selectById(Branch.Id("b1")))
    assertEquals(branchRow("b2", createdAt = 2), dao.selectById(Branch.Id("b2")))
  }

  private suspend fun upsertBranch(name: String) {
    dao.upsert(
      id = Branch.Id("b1"),
      conversationId = Conversation.Id("c1"),
      parentBranchId = Branch.Id("main"),
      branchedFromCommitId = Commit.Id("commit"),
      name = name,
      lastCommit = null,
      lastCommitTimestamp = 0,
      createdAt = 1,
      createdById = UserId("author")
    )
  }

  private fun branchRow(id: String, createdAt: Long): BranchRow {
    return BranchRow(
      id = Branch.Id(id),
      conversationId = Conversation.Id("c1"),
      parentBranchId = Branch.Id("main"),
      branchedFromCommitId = Commit.Id("commit"),
      name = "$id name",
      lastCommit = null,
      lastCommitTimestamp = 0,
      unreadCount = 0,
      createdAt = createdAt,
      createdById = UserId("author"),
      mergeRequestStatus = null,
      mergeRequestInitiatorId = null,
      mergeRequestRequestedAt = null,
      mergeRequestApprovedByIds = null,
      mergeRequestMergedAt = null,
      mergeRequestMergedIntoBranchId = null
    )
  }

  private fun mergedBranchRow(id: String, createdAt: Long): BranchRow {
    return branchRow(id, createdAt).copy(
      mergeRequestStatus = "merged",
      mergeRequestInitiatorId = UserId("author"),
      mergeRequestRequestedAt = 10,
      mergeRequestApprovedByIds = setOf(UserId("alice"), UserId("bob")),
      mergeRequestMergedAt = 20,
      mergeRequestMergedIntoBranchId = Branch.Id("main")
    )
  }

  private suspend fun insertConversation(id: String) {
    execute("INSERT INTO ChatConversation (id, type, lastCommitTimestamp) VALUES ('$id', 'group', 0)")
  }

  private suspend fun insertBranch(id: String, conversationId: String, createdAt: Long) {
    execute(
      """
      INSERT INTO ChatBranch (
        id, conversationId, parentBranchId, branchedFromCommitId, name, lastCommitTimestamp,
        createdAt, createdById
      )
      VALUES ('$id', '$conversationId', 'main', 'commit', '$id name', 0, $createdAt, 'author')
      """
    )
  }

  private suspend fun insertMergeRequest(branchId: String) {
    execute(
      """
      INSERT INTO MergeRequest (
        branchId, status, initiatorId, requestedAt, approvedByIds, mergedAt, mergedIntoBranchId
      )
      VALUES ('$branchId', 'merged', 'author', 10, '["alice","bob"]', 20, 'main')
      """
    )
  }

  private suspend fun execute(sql: String) {
    database.useWriterConnection { it.executeSQL(sql) }
  }
}
