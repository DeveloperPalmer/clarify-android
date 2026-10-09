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
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.database.entity.CommitCursorRow
import ru.sla.clarify.database.entity.CommitEditStateRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

class ChatCommitDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: ChatCommitDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.chatCommitDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `select and observe return the branch feed newest first, ties by id`() = runTest {
    insertConversation("c1")
    insertCommit("a", branchId = "b1", createdAtNanos = 1)
    insertCommit("c", branchId = "b1", createdAtNanos = 2)
    insertCommit("b", branchId = "b1", createdAtNanos = 2)
    insertCommit("other", branchId = "b2", createdAtNanos = 3)
    val expected = listOf(Commit.Id("c"), Commit.Id("b"), Commit.Id("a"))

    assertEquals(expected, dao.select(Conversation.Id("c1"), Branch.Id("b1")).map { it.id })
    dao.observe(Conversation.Id("c1"), Branch.Id("b1")).test {
      assertEquals(expected, awaitItem().map { it.id })
    }
  }

  @Test
  fun `observeByConversation skips commits without server time, oldest first`() = runTest {
    insertConversation("c1")
    insertCommit("pending", branchId = "b1", createdAtNanos = 0)
    insertCommit("late", branchId = "b2", createdAtNanos = 2)
    insertCommit("early", branchId = "b1", createdAtNanos = 1)

    dao.observeByConversation(Conversation.Id("c1")).test {
      assertEquals(
        listOf("early" to "b1", "late" to "b2"),
        awaitItem().map { it.id.value to it.branchId.value }
      )
    }
  }

  @Test
  fun `oldest cursor is the earliest commit of the branch with server time`() = runTest {
    insertConversation("c1")
    insertCommit("pending", branchId = "b1", createdAtNanos = 0)
    insertCommit("second", branchId = "b1", createdAtNanos = 2)
    insertCommit("first", branchId = "b1", createdAtNanos = 1)
    insertCommit("other", branchId = "b2", createdAtNanos = 0)
    val expected = CommitCursorRow(id = Commit.Id("first"), createdAtNanos = 1)

    assertEquals(expected, dao.selectOldestCursor(Conversation.Id("c1"), Branch.Id("b1")))
    assertNull(dao.selectOldestCursor(Conversation.Id("c1"), Branch.Id("b2")))
    dao.observeOldestCursor(Conversation.Id("c1"), Branch.Id("b1")).test {
      assertEquals(expected, awaitItem())
    }
  }

  @Test
  fun `selectEditState returns the editable fields`() = runTest {
    insertConversation("c1")
    insertCommit("a", branchId = "b1", createdAtNanos = 1)

    assertEquals(
      CommitEditStateRow(text = "a text", editedAtNanos = null, status = "sent"),
      dao.selectEditState(Commit.Id("a"))
    )
    assertNull(dao.selectEditState(Commit.Id("missing")))
  }

  @Test
  fun `selectByIds returns whole rows for the given ids`() = runTest {
    insertConversation("c1")
    insertCommit("a", branchId = "b1", createdAtNanos = 1)
    insertCommit("b", branchId = "b1", createdAtNanos = 2, withReply = true)

    assertEquals(emptyList<ChatCommitEntity>(), dao.selectByIds(emptyList()))
    assertEquals(
      listOf(
        ChatCommitEntity(
          id = Commit.Id("b"),
          conversationId = Conversation.Id("c1"),
          branchId = Branch.Id("b1"),
          senderId = UserId("alice"),
          type = "text",
          text = "b text",
          replyCommit = Commit.Reply(
            id = Commit.Id("a"),
            senderId = UserId("alice"),
            isSelf = true,
            text = "a text"
          ),
          invitedId = null,
          createdAtNanos = 2,
          isSelf = true,
          status = "sent",
          editedAtNanos = null
        )
      ),
      dao.selectByIds(listOf(Commit.Id("b"), Commit.Id("missing")))
    )
  }

  @Test
  fun `insertOrReplace stores every column and replaces by id`() = runTest {
    insertConversation("c1")
    val commit = ChatCommitEntity(
      id = Commit.Id("a"),
      conversationId = Conversation.Id("c1"),
      branchId = Branch.Id("b1"),
      senderId = UserId("alice"),
      type = "invite",
      text = "hello",
      replyCommit = Commit.Reply(
        id = Commit.Id("z"),
        senderId = UserId("bob"),
        isSelf = false,
        text = "quoted"
      ),
      invitedId = UserId("carol"),
      createdAtNanos = 5,
      isSelf = true,
      status = "sending",
      editedAtNanos = 7
    )

    dao.insertOrReplace(commit)
    assertEquals(listOf(commit), dao.selectByIds(listOf(Commit.Id("a"))))

    dao.insertOrReplace(commit.copy(status = "sent", invitedId = null))
    assertEquals(
      listOf(commit.copy(status = "sent", invitedId = null)),
      dao.selectByIds(listOf(Commit.Id("a")))
    )
  }

  private suspend fun insertConversation(id: String) {
    execute("INSERT INTO ChatConversation (id, type, lastCommitTimestamp) VALUES ('$id', 'group', 0)")
  }

  private suspend fun insertCommit(
    id: String,
    branchId: String,
    createdAtNanos: Long,
    withReply: Boolean = false
  ) {
    val reply = if (withReply) {
      """'{"id":"a","senderId":"alice","isSelf":true,"text":"a text"}'"""
    } else {
      "NULL"
    }
    execute(
      """
      INSERT INTO ChatCommit (
        id, conversationId, branchId, senderId, text, replyCommit, createdAtNanos, isSelf, status
      )
      VALUES ('$id', 'c1', '$branchId', 'alice', '$id text', $reply, $createdAtNanos, 1, 'sent')
      """
    )
  }

  private suspend fun execute(sql: String) {
    database.useWriterConnection { it.executeSQL(sql) }
  }
}
