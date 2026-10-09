package ru.sla.clarify.database

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

/**
 * Внешние ключи в схеме чата объявлены с каскадом, и Room включает их при открытии базы. Отсюда два
 * следствия, которые держат эти тесты: ребёнок без родителя не записывается, а удаление родителя —
 * в том числе скрытое внутри `INSERT OR REPLACE` — уносит его детей.
 */
class ChatDatabaseForeignKeysTest {

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

  /**
   * Текст ошибки не проверяется: в JVM-тесте она приходит заглушкой `android.database.SQLException`
   * из `android.jar`, без сообщения. Поэтому проверяется результат — исключение есть, строки нет.
   */
  @Test
  fun `commit without its conversation is rejected`() = runTest {
    val error = runCatching { database.chatCommitDao().insertOrReplace(commit()) }.exceptionOrNull()

    assertNotNull(error)
    assertEquals(0L, count("ChatCommit"))
  }

  @Test
  fun `upsert of a conversation keeps its branches, commits and merge requests`() = runTest {
    upsertConversation()
    insertChildren()

    upsertConversation(lastCommit = "updated")

    assertChildrenPresent()
  }

  /**
   * Та же запись строкой `INSERT OR REPLACE` — это удаление и вставка: каскад уносит детей. Ради
   * этого родители пишутся через `upsert`.
   */
  @Test
  fun `insert or replace of a conversation cascades to its children`() = runTest {
    upsertConversation()
    insertChildren()

    execute("INSERT OR REPLACE INTO ChatConversation (id, type, lastCommitTimestamp) VALUES ('c1', 'group', 0)")

    assertChildrenGone()
  }

  @Test
  fun `deleting a conversation cascades to branches, commits and merge requests`() = runTest {
    upsertConversation()
    insertChildren()

    database.chatConversationDao().delete(Conversation.Id("c1"))

    assertChildrenGone()
  }

  @Test
  fun `deleting a branch cascades to its merge request only`() = runTest {
    upsertConversation()
    insertChildren()

    database.chatBranchDao().delete(Branch.Id("b1"))

    assertNull(database.chatBranchDao().selectById(Branch.Id("b1")))
    assertEquals(0L, count("MergeRequest"))
    assertEquals(listOf(Commit.Id("a")), selectCommitIds())
  }

  private suspend fun upsertConversation(lastCommit: String? = null) {
    database.chatConversationDao().upsert(
      id = Conversation.Id("c1"),
      type = "group",
      name = "group",
      ownerId = UserId("owner"),
      lastCommit = lastCommit,
      lastCommitSenderId = null,
      lastCommitTimestamp = 0
    )
  }

  private suspend fun insertChildren() {
    database.chatBranchDao().upsert(
      id = Branch.Id("b1"),
      conversationId = Conversation.Id("c1"),
      parentBranchId = Branch.Id("main"),
      branchedFromCommitId = Commit.Id("a"),
      name = "branch",
      lastCommit = null,
      lastCommitTimestamp = 0,
      createdAt = 1,
      createdById = UserId("author")
    )
    execute(
      """
      INSERT INTO MergeRequest (branchId, status, initiatorId, requestedAt, approvedByIds)
      VALUES ('b1', 'open', 'author', 1, '[]')
      """
    )
    database.chatCommitDao().insertOrReplace(commit())
  }

  private suspend fun assertChildrenPresent() {
    assertNotNull(database.chatBranchDao().selectById(Branch.Id("b1")))
    assertEquals(1L, count("MergeRequest"))
    assertEquals(listOf(Commit.Id("a")), selectCommitIds())
  }

  private suspend fun assertChildrenGone() {
    assertEquals(0L, count("ChatBranch"))
    assertEquals(0L, count("MergeRequest"))
    assertEquals(0L, count("ChatCommit"))
  }

  private suspend fun selectCommitIds(): List<Commit.Id> {
    return database.chatCommitDao().select(Conversation.Id("c1"), Branch.Id("main")).map { it.id }
  }

  private suspend fun count(table: String): Long {
    return database.useWriterConnection { connection ->
      connection.usePrepared("SELECT COUNT(*) FROM $table") { statement ->
        statement.step()
        statement.getLong(0)
      }
    }
  }

  private fun commit(): ChatCommitEntity {
    return ChatCommitEntity(
      id = Commit.Id("a"),
      conversationId = Conversation.Id("c1"),
      branchId = Branch.Id("main"),
      senderId = UserId("alice"),
      type = "text",
      text = "hello",
      replyCommit = null,
      invitedId = null,
      createdAtNanos = 1,
      isSelf = false,
      status = "sent",
      editedAtNanos = null
    )
  }

  private suspend fun execute(sql: String) {
    database.useWriterConnection { it.executeSQL(sql) }
  }
}
