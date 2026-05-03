package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import java.time.LocalTime

object LocalTimeAdapter : ColumnAdapter<LocalTime, Long> {
  override fun decode(databaseValue: Long): LocalTime {
    return LocalTime.ofSecondOfDay(databaseValue)
  }

  override fun encode(value: LocalTime): Long {
    return value.toSecondOfDay().toLong()
  }
}
