package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import java.time.LocalDate

object LocalDateAdapter : ColumnAdapter<LocalDate, Long> {
  override fun decode(databaseValue: Long): LocalDate {
    return LocalDate.ofEpochDay(databaseValue)
  }

  override fun encode(value: LocalDate): Long {
    return value.toEpochDay()
  }
}
