package ru.sla.clarify.core.domain.date

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats time as "10:35"
 */
val TIME_FORMATTER_HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Formats date as "12 мая", "13 июля"
 */
val DATE_FORMATTER_DAY_MONTH_NAME: DateTimeFormatter =
  DateTimeFormatter.ofPattern("d MMMM", Locale("ru", "RU"))

/**
 * Formats date as "12 мая 2023", "13 июля 2024"
 */
val DATE_FORMATTER_DAY_MONTH_NAME_YEAR: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru", "RU"))

/**
 * Formats date as "12.05.2023"
 */
val DATE_FORMATTER_DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Formats date as "май 2023", "июль 2023"
 */
val DATE_FORMATTER_MONTH_NAME_YEAR: DateTimeFormatter =
  DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))

/**
 * Formats date as "12 мая, 13:48"
 */
val DATE_TIME_FORMATTER_DAY_MONTH_TIME: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd MMMM, HH:mm", Locale("ru", "RU"))
