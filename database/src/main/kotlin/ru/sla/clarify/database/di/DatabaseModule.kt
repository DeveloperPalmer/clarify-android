package ru.sla.clarify.database.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.adapter.StringListAdapter
import ru.sla.clarify.database.chat.MergeRequest
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(AppScope::class)
interface DatabaseModule {
  @SingleIn(AppScope::class)
  @Provides
  fun provideInMemoryDatabase(@ApplicationContext context: Context): InMemoryDB {
    val driver = AndroidSqliteDriver(InMemoryDB.Schema, context, name = null)
    return InMemoryDB(
      driver = driver,
      MergeRequestAdapter = MergeRequest.Adapter(
        approvedByUidsAdapter = StringListAdapter
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
