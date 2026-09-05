package ru.sla.clarify.core.domain.date

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private const val NANOS_PER_SECOND = 1_000_000_000L

fun Long.toLocalDateTime(): LocalDateTime {
  return Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
}

fun nowEpochNanos(): Long {
  val now = Instant.now()
  return now.epochSecond * NANOS_PER_SECOND + now.nano
}
