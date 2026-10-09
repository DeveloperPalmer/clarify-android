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
import ru.sla.clarify.database.entity.UserEntity

class UserDaoTest {

  private lateinit var database: ChatDatabase
  private lateinit var dao: UserDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<ChatDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.userDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `observe returns the user by id`() = runTest {
    insertUser("alice", photoUrl = null)
    insertUser("bob", photoUrl = "https://photo")

    dao.observe(UserId("bob")).test {
      assertEquals(
        UserEntity(
          id = UserId("bob"),
          email = "bob@mail",
          displayName = "bob name",
          photoUrl = "https://photo"
        ),
        awaitItem()
      )
    }
  }

  @Test
  fun `observe returns null for unknown id`() = runTest {
    dao.observe(UserId("missing")).test {
      assertNull(awaitItem())
    }
  }

  /**
   * Запись в таблицу, не меняющая выборку, не должна доходить до подписчика: следующим приходит
   * уже изменённая строка, а не повтор прежней.
   */
  @Test
  fun `observe skips writes that leave the result unchanged`() = runTest {
    insertUser("alice", photoUrl = null)

    dao.observe(UserId("alice")).test {
      assertNull(awaitItem()?.photoUrl)

      insertUser("bob", photoUrl = null)
      execute("UPDATE User SET photoUrl = 'https://photo' WHERE id = 'alice'")

      assertEquals("https://photo", awaitItem()?.photoUrl)
    }
  }

  @Test
  fun `insertOrReplace replaces the profile of the same user`() = runTest {
    val user = UserEntity(id = UserId("alice"), email = "a@mail", displayName = "Alice", photoUrl = null)

    dao.insertOrReplace(user)
    dao.insertOrReplace(user.copy(displayName = "Alice B"))

    dao.observe(UserId("alice")).test {
      assertEquals(user.copy(displayName = "Alice B"), awaitItem())
    }
  }

  private suspend fun insertUser(id: String, photoUrl: String?) {
    val photo = photoUrl?.let { "'$it'" } ?: "NULL"
    execute("INSERT INTO User VALUES ('$id', '$id@mail', '$id name', $photo)")
  }

  private suspend fun execute(sql: String) {
    database.useWriterConnection { it.executeSQL(sql) }
  }
}
