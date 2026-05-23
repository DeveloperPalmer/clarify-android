package ru.sla.clarify.database.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.adapter.StringListAdapter
import ru.sla.clarify.database.chat.Branch
import ru.sla.clarify.database.chat.ChatConversation

@ContributesTo(AppScope::class)
@Module
object DatabaseModule {
  @SingleIn(AppScope::class)
  @Provides
  fun provideInMemoryDatabase(@ApplicationContext context: Context): InMemoryDB {
    val driver = AndroidSqliteDriver(InMemoryDB.Schema, context, name = null)
    return InMemoryDB(
      driver = driver,
      ChatConversationAdapter = ChatConversation.Adapter(
        participantUidsAdapter = StringListAdapter
      ),
      BranchAdapter = Branch.Adapter(
        mergeRequestApprovedByUidsAdapter = StringListAdapter
      )
    )
  }

  @SingleIn(AppScope::class)
  @Provides
  fun providePersistedDatabase(@ApplicationContext context: Context): PersistedDB {
    val driver = AndroidSqliteDriver(PersistedDB.Schema, context, name = "clarify.db")
    return PersistedDB(driver)
  }
}
