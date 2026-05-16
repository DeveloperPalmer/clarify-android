package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.feature.entity.chat.Commit

object CommitIdAdapter : ColumnAdapter<Commit.Id, String> {
  override fun decode(databaseValue: String): Commit.Id {
    return Commit.Id(databaseValue)
  }

  override fun encode(value: Commit.Id): String {
    return value.value
  }
}
