package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object OffsetDateTimeAdapter : ColumnAdapter<OffsetDateTime, String> {
  override fun decode(databaseValue: String): OffsetDateTime {
    return OffsetDateTime.parse(databaseValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  override fun encode(value: OffsetDateTime): String {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)
  }
}
