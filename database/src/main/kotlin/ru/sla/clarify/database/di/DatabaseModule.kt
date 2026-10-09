package ru.sla.clarify.database.di

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.SettingsDatabase
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(AppScope::class)
interface DatabaseModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsDatabase(@ApplicationContext context: Context): SettingsDatabase {
    return Room.databaseBuilder(context, SettingsDatabase::class.java, "settings.db")
      .setDriver(AndroidSQLiteDriver())
      .addCallback(
        object : RoomDatabase.Callback() {
          // Прежние версии хранили Settings в clarify.db, и вместе с ними там остались токены сессии.
          // Этот файл больше никто не открывает и выход из аккаунта его не чистит, поэтому он удаляется,
          // когда впервые создаётся settings.db. Колбэк не нужен, когда не останется установок, где
          // Settings жили в clarify.db.
          override suspend fun onCreate(connection: SQLiteConnection) {
            context.deleteDatabase("clarify.db")
          }
        }
      )
      .build()
  }

  /**
   * Кэш чата живёт только вместе с процессом: базу в памяти не закрывают — закрытие стирает данные.
   */
  @SingleIn(AppScope::class)
  @Provides
  fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
    return Room.inMemoryDatabaseBuilder(context, ChatDatabase::class.java)
      .setDriver(AndroidSQLiteDriver())
      .build()
  }
}
