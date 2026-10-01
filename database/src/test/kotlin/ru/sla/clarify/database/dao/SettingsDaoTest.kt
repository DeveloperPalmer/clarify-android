package ru.sla.clarify.database.dao

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sla.clarify.database.SettingsDatabase
import ru.sla.clarify.database.entity.SettingsEntity
import ru.sla.clarify.database.entity.SettingsEntity.Key

/**
 * База в памяти поднимается без эмулятора: драйвер подменён на SQLite, собранный под хост-JVM.
 * Тест `observe` обязателен — он ловит ситуацию, когда запись и подписка попадают в разные
 * соединения, а значит, в разные базы.
 */
class SettingsDaoTest {

  private lateinit var database: SettingsDatabase
  private lateinit var dao: SettingsDao

  @BeforeEach
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder<SettingsDatabase>()
      .setDriver(BundledSQLiteDriver())
      .build()
    dao = database.settingsDao()
  }

  @AfterEach
  fun tearDown() {
    database.close()
  }

  @Test
  fun `select returns null for unknown key`() = runTest {
    assertNull(dao.select(Key("missing")))
  }

  @Test
  fun `insert then select returns the value`() = runTest {
    dao.insertOrReplace(SettingsEntity(key = Key("session_key"), value = "abc"))

    assertEquals("abc", dao.select(Key("session_key")))
  }

  @Test
  fun `insert of the same key replaces the value`() = runTest {
    dao.insertOrReplace(SettingsEntity(key = Key("k"), value = "old"))
    dao.insertOrReplace(SettingsEntity(key = Key("k"), value = "new"))

    assertEquals("new", dao.select(Key("k")))
  }

  @Test
  fun `delete removes only the given key`() = runTest {
    dao.insertOrReplace(SettingsEntity(key = Key("a"), value = "1"))
    dao.insertOrReplace(SettingsEntity(key = Key("b"), value = "2"))

    dao.delete(Key("a"))

    assertNull(dao.select(Key("a")))
    assertEquals("2", dao.select(Key("b")))
  }

  @Test
  fun `observe emits null first and then the written value`() = runTest {
    dao.observe(Key("k")).test {
      assertNull(awaitItem())

      dao.insertOrReplace(SettingsEntity(key = Key("k"), value = "v"))

      assertEquals("v", awaitItem())
    }
  }
}
