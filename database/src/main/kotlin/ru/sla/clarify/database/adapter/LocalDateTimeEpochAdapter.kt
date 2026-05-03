package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalDateTimeEpochAdapter : ColumnAdapter<LocalDateTime, Long> {
  override fun decode(databaseValue: Long): LocalDateTime {
    return LocalDateTime.ofEpochSecond(databaseValue, 0, ZoneOffset.UTC)
  }

  override fun encode(value: LocalDateTime): Long {
    return value.toEpochSecond(ZoneOffset.UTC)
  }
}
