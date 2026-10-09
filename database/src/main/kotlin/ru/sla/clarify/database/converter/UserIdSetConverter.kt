package ru.sla.clarify.database.converter

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json
import ru.sla.clarify.core.domain.entity.UserId

object UserIdSetConverter {
  @ColumnTypeConverter
  fun decode(databaseValue: String): Set<UserId> {
    return Json.decodeFromString<Set<String>>(databaseValue).mapTo(mutableSetOf(), ::UserId)
  }

  @ColumnTypeConverter
  fun encode(value: Set<UserId>): String {
    return Json.encodeToString(value.map { it.value })
  }
}
