package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json
import ru.sla.clarify.core.domain.entity.UserId

object UserIdSetAdapter : ColumnAdapter<Set<UserId>, String> {
  override fun decode(databaseValue: String): Set<UserId> {
    return Json.decodeFromString<Set<String>>(databaseValue).mapTo(mutableSetOf(), ::UserId)
  }

  override fun encode(value: Set<UserId>): String {
    return Json.encodeToString(value.map { it.value })
  }
}
