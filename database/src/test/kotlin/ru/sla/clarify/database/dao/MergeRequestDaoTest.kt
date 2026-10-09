package ru.sla.clarify.database.dao

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.MergeRequestEntity
import ru.sla.clarify.entity.chat.Branch

/**
 * Своих чтений у запроса слияния нет, поэтому записанное проверяется через ветку.
 */
class MergeRequestDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: MergeRequestDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.mergeRequestDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `insertOrReplace stores the request and replaces it by branch`() = runTest {
    insertBranch()
    val request = MergeRequestEntity(
      branchId = Branch.Id("b1"),
      status = "open",
      initiatorId = UserId("author"),
      requestedAt = 10,
      approvedByIds = emptySet(),
      mergedAt = null,
      mergedIntoBranchId = null
    )

    dao.insertOrReplace(request)
    dao.insertOrReplace(
      request.copy(
        status = "merged",
        approvedByIds = setOf(UserId("alice")),
        mergedAt = 20,
        mergedIntoBranchId = Branch.Id("main")
      )
    )

    val branch = database.chatBranchDao().selectById(Branch.Id("b1"))
    assertEquals("merged", branch?.mergeRequestStatus)
    assertEquals(setOf(UserId("alice")), branch?.mergeRequestApprovedByIds)
    assertEquals(20L, branch?.mergeRequestMergedAt)
    assertEquals(Branch.Id("main"), branch?.mergeRequestMergedIntoBranchId)
  }

  @Test
  fun `delete removes the request but keeps the branch`() = runTest {
    insertBranch()
    dao.insertOrReplace(
      MergeRequestEntity(
        branchId = Branch.Id("b1"),
        status = "open",
        initiatorId = UserId("author"),
        requestedAt = 10,
        approvedByIds = emptySet(),
        mergedAt = null,
        mergedIntoBranchId = null
      )
    )

    dao.delete(Branch.Id("b1"))

    val branch = database.chatBranchDao().selectById(Branch.Id("b1"))
    assertEquals(Branch.Id("b1"), branch?.id)
    assertNull(branch?.mergeRequestStatus)
  }

  private suspend fun insertBranch() {
    database.useWriterConnection {
      it.executeSQL("INSERT INTO ChatConversation (id, type, lastCommitTimestamp) VALUES ('c1', 'group', 0)")
      it.executeSQL(
        """
        INSERT INTO ChatBranch (
          id, conversationId, parentBranchId, branchedFromCommitId, name, lastCommitTimestamp,
          createdAt, createdById
        )
        VALUES ('b1', 'c1', 'main', 'commit', 'b1 name', 0, 1, 'author')
        """
      )
    }
  }
}
