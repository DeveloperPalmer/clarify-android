package ru.sla.clarify.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime {
  return Instant
    .ofEpochSecond(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
}
