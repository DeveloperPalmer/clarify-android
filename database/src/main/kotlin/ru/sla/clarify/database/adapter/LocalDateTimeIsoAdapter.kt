package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeIsoAdapter : ColumnAdapter<LocalDateTime, String> {
  override fun decode(databaseValue: String): LocalDateTime {
    return LocalDateTime.parse(databaseValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  override fun encode(value: LocalDateTime): String {
    return value.toString()
  }
}
