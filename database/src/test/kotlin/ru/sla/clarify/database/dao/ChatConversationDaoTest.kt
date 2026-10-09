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
import ru.sla.clarify.database.entity.DirectConversationRow
import ru.sla.clarify.database.entity.GroupConversationRow
import ru.sla.clarify.database.entity.GroupRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

class ChatConversationDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: ChatConversationDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.chatConversationDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `observeDirects shows the peer, not the current user, newest first`() = runTest {
    insertConversation("d1", "direct", timestamp = 1)
    insertConversation("d2", "direct", timestamp = 2)
    insertConversation("g1", "group", timestamp = 3)
    insertMember("d1", "me")
    insertMember("d1", "alice")
    insertMember("d2", "me")
    insertMember("d2", "bob")
    insertMember("g1", "carol")
    insertUser("me")
    insertUser("alice")
    insertUser("bob")
    insertUser("carol")

    dao.observeDirects(Member.Id("me")).test {
      assertEquals(
        listOf(
          directRow("d2", timestamp = 2, peer = "bob"),
          directRow("d1", timestamp = 1, peer = "alice")
        ),
        awaitItem()
      )
    }
  }

  @Test
  fun `observeDirects skips a conversation whose peer has no profile yet`() = runTest {
    insertConversation("d1", "direct", timestamp = 1)
    insertMember("d1", "me")
    insertMember("d1", "alice")
    insertUser("me")

    dao.observeDirects(Member.Id("me")).test {
      assertEquals(emptyList<DirectConversationRow>(), awaitItem())
    }
  }

  @Test
  fun `observeGroups counts members and resolves the last sender, newest first`() = runTest {
    insertConversation("g1", "group", timestamp = 1, name = "first", senderId = "alice")
    insertConversation("g2", "group", timestamp = 2, name = "second", senderId = "ghost")
    insertConversation("d1", "direct", timestamp = 3)
    insertMember("g1", "alice")
    insertMember("g1", "bob")
    insertMember("g2", "alice")
    insertUser("alice")

    dao.observeGroups().test {
      assertEquals(
        listOf(
          groupConversationRow(
            "g2",
            timestamp = 2,
            name = "second",
            senderId = "ghost",
            memberCount = 1,
            senderName = null
          ),
          groupConversationRow(
            "g1",
            timestamp = 1,
            name = "first",
            senderId = "alice",
            memberCount = 2,
            senderName = "alice name"
          )
        ),
        awaitItem()
      )
    }
  }

  @Test
  fun `observeGroup returns only a group`() = runTest {
    insertConversation("g1", "group", timestamp = 1, name = "group", senderId = "alice")
    insertConversation("d1", "direct", timestamp = 2)
    insertMember("g1", "alice")
    insertMember("g1", "bob")

    dao.observeGroup(Conversation.Id("g1")).test {
      assertEquals(
        GroupRow(
          id = Conversation.Id("g1"),
          name = "group",
          ownerId = UserId("owner"),
          lastCommit = null,
          lastCommitSenderId = UserId("alice"),
          lastCommitTimestamp = 1,
          unreadCount = 0,
          memberCount = 2
        ),
        awaitItem()
      )
    }
    dao.observeGroup(Conversation.Id("d1")).test {
      assertNull(awaitItem())
    }
  }

  @Test
  fun `observeIds returns every conversation`() = runTest {
    insertConversation("d1", "direct", timestamp = 1)
    insertConversation("g1", "group", timestamp = 2)

    dao.observeIds().test {
      assertEquals(setOf(Conversation.Id("d1"), Conversation.Id("g1")), awaitItem().toSet())
    }
  }

  /**
   * Разговор находится только по полному набору участников: и меньший, и больший набор — другой
   * разговор.
   */
  @Test
  fun `selectIdByMembers matches the exact member set only`() = runTest {
    insertConversation("pair", "direct", timestamp = 1)
    insertConversation("trio", "direct", timestamp = 2)
    insertConversation("group", "group", timestamp = 3)
    insertMember("pair", "me")
    insertMember("pair", "alice")
    insertMember("trio", "me")
    insertMember("trio", "alice")
    insertMember("trio", "bob")
    insertMember("group", "me")
    insertMember("group", "carol")

    assertEquals(Conversation.Id("pair"), selectIdByMembers("direct", "me", "alice"))
    assertEquals(Conversation.Id("trio"), selectIdByMembers("direct", "me", "alice", "bob"))
    assertNull(selectIdByMembers("direct", "me"))
    assertNull(selectIdByMembers("direct", "me", "carol"))
    dao.observeIdByMembers("direct", listOf(Member.Id("me"), Member.Id("alice")), 2).test {
      assertEquals(Conversation.Id("pair"), awaitItem())
    }
  }

  /**
   * Повторная запись разговора обновляет его поля, но не трогает локальный счётчик непрочитанного.
   */
  @Test
  fun `upsert updates the row and keeps unreadCount`() = runTest {
    upsertGroup(name = "old")
    execute("UPDATE ChatConversation SET unreadCount = 5 WHERE id = 'g1'")

    upsertGroup(name = "new")

    dao.observeGroup(Conversation.Id("g1")).test {
      val group = awaitItem()
      assertEquals("new", group?.name)
      assertEquals(5L, group?.unreadCount)
    }
  }

  @Test
  fun `upsert of a new conversation starts with zero unread`() = runTest {
    upsertGroup(name = "new")

    dao.observeGroup(Conversation.Id("g1")).test {
      assertEquals(0L, awaitItem()?.unreadCount)
    }
  }

  @Test
  fun `updateUnreadCount changes only the counter of the given conversation`() = runTest {
    upsertGroup(name = "group")
    insertConversation("g2", "group", timestamp = 2, name = "other")

    dao.updateUnreadCount(Conversation.Id("g1"), 4)

    dao.observeGroups().test {
      assertEquals(
        mapOf("other" to 0L, "group" to 4L),
        awaitItem().associate { it.name to it.unreadCount }
      )
    }
  }

  @Test
  fun `updateName changes only the name`() = runTest {
    upsertGroup(name = "old")
    execute("UPDATE ChatConversation SET unreadCount = 2 WHERE id = 'g1'")

    dao.updateName(Conversation.Id("g1"), "new")

    dao.observeGroup(Conversation.Id("g1")).test {
      assertEquals(
        GroupRow(
          id = Conversation.Id("g1"),
          name = "new",
          ownerId = UserId("owner"),
          lastCommit = null,
          lastCommitSenderId = null,
          lastCommitTimestamp = 1,
          unreadCount = 2,
          memberCount = 0
        ),
        awaitItem()
      )
    }
  }

  private suspend fun upsertGroup(name: String) {
    dao.upsert(
      id = Conversation.Id("g1"),
      type = "group",
      name = name,
      ownerId = UserId("owner"),
      lastCommit = null,
      lastCommitSenderId = null,
      lastCommitTimestamp = 1
    )
  }

  private suspend fun selectIdByMembers(type: String, vararg ids: String): Conversation.Id? {
    return dao.selectIdByMembers(type, ids.map(Member::Id), ids.size.toLong())
  }

  private fun directRow(id: String, timestamp: Long, peer: String): DirectConversationRow {
    return DirectConversationRow(
      id = Conversation.Id(id),
      type = "direct",
      lastCommit = null,
      lastCommitTimestamp = timestamp,
      unreadCount = 0,
      peerId = Member.Id(peer),
      peerDisplayName = "$peer name",
      peerPhotoUrl = null
    )
  }

  private fun groupConversationRow(
    id: String,
    timestamp: Long,
    name: String,
    senderId: String,
    memberCount: Long,
    senderName: String?
  ): GroupConversationRow {
    return GroupConversationRow(
      id = Conversation.Id(id),
      name = name,
      ownerId = UserId("owner"),
      lastCommit = null,
      lastCommitSenderId = UserId(senderId),
      lastCommitTimestamp = timestamp,
      unreadCount = 0,
      memberCount = memberCount,
      lastCommitSenderDisplayName = senderName
    )
  }

  private suspend fun insertConversation(
    id: String,
    type: String,
    timestamp: Long,
    name: String? = null,
    senderId: String? = null
  ) {
    val owner = if (type == "group") "'owner'" else "NULL"
    execute(
      """
      INSERT INTO ChatConversation (id, type, name, ownerId, lastCommitSenderId, lastCommitTimestamp)
      VALUES ('$id', '$type', ${name.toSql()}, $owner, ${senderId.toSql()}, $timestamp)
      """
    )
  }

  private suspend fun insertMember(conversationId: String, id: String) {
    execute("INSERT INTO ChatMember (id, conversationId) VALUES ('$id', '$conversationId')")
  }

  private suspend fun insertUser(id: String) {
    execute("INSERT INTO User VALUES ('$id', '$id@mail', '$id name', NULL)")
  }

  private suspend fun execute(sql: String) {
    database.useWriterConnection { it.executeSQL(sql) }
  }

  private fun String?.toSql(): String {
    return this?.let { "'$it'" } ?: "NULL"
  }
}
