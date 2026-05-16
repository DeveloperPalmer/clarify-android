package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.auth.session.domain.entity.UserId

object UserIdAdapter : ColumnAdapter<UserId, String> {
  override fun decode(databaseValue: String): UserId {
    return UserId(databaseValue)
  }

  override fun encode(value: UserId): String {
    return value.value
  }
}
