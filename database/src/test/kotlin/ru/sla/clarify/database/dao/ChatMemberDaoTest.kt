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
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.DirectMemberRow
import ru.sla.clarify.database.entity.GroupMemberRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

class ChatMemberDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: ChatMemberDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.chatMemberDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `observeWithoutProfile returns members lacking a profile except the current user`() = runTest {
    insertMember("c1", "me")
    insertMember("c1", "alice")
    insertMember("c1", "bob")
    insertMember("c2", "bob")
    insertUser("alice")

    dao.observeWithoutProfile(Member.Id("me")).test {
      assertEquals(listOf(Member.Id("bob")), awaitItem())
    }
  }

  @Test
  fun `selectIds returns members of the conversation only`() = runTest {
    insertMember("c1", "alice")
    insertMember("c1", "bob")
    insertMember("c2", "carol")

    assertEquals(
      setOf(Member.Id("alice"), Member.Id("bob")),
      dao.selectIds(Conversation.Id("c1")).toSet()
    )
  }

  @Test
  fun `observeGroup keeps members without a profile`() = runTest {
    insertMember("c1", "alice")
    insertMember("c1", "bob")
    insertUser("alice")

    dao.observeGroup(Conversation.Id("c1")).test {
      assertEquals(
        setOf(
          GroupMemberRow(
            id = Member.Id("alice"),
            displayName = "alice name",
            email = "alice@mail",
            photoUrl = null
          ),
          GroupMemberRow(
            id = Member.Id("bob"),
            displayName = null,
            email = null,
            photoUrl = null
          )
        ),
        awaitItem().toSet()
      )
    }
  }

  @Test
  fun `selectDirect and observeDirect return the same rows`() = runTest {
    insertMember("c1", "me")
    insertMember("c1", "alice")
    insertUser("alice")
    val expected = setOf(
      DirectMemberRow(id = Member.Id("me"), displayName = null, photoUrl = null),
      DirectMemberRow(id = Member.Id("alice"), displayName = "alice name", photoUrl = null)
    )

    assertEquals(expected, dao.selectDirect(Conversation.Id("c1")).toSet())
    dao.observeDirect(Conversation.Id("c1")).test {
      assertEquals(expected, awaitItem().toSet())
    }
  }

  @Test
  fun `observeDirectById returns the member of the given conversation`() = runTest {
    insertMember("c1", "alice")
    insertMember("c2", "bob")
    insertUser("alice")

    dao.observeDirectById(Conversation.Id("c1"), Member.Id("alice")).test {
      assertEquals(
        DirectMemberRow(id = Member.Id("alice"), displayName = "alice name", photoUrl = null),
        awaitItem()
      )
    }
    dao.observeDirectById(Conversation.Id("c1"), Member.Id("bob")).test {
      assertNull(awaitItem())
    }
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
}
