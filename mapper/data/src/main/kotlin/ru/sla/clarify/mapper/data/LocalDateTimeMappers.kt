package ru.sla.clarify.mapper.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime {
  return Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
}
