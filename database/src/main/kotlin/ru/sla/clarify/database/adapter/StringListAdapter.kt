package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json

typealias StringList = List<String>

object StringListAdapter : ColumnAdapter<StringList, String> {
  override fun decode(databaseValue: String): StringList {
    return Json.decodeFromString(databaseValue)
  }

  override fun encode(value: StringList): String {
    return Json.encodeToString(value)
  }
}
