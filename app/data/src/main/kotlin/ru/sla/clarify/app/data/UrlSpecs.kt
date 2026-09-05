package ru.sla.clarify.app.data

import ru.kode.pathfinder.UrlSpec

/**
 * Адреса операций, которые PathFinder умеет переопределять поштучно.
 *
 * Пока API не описан спекой, переопределять нечего, и список пуст. Наполняется, когда генератор
 * начнёт печатать перечисления адресов — по одному на тег.
 */
internal fun buildUrlSpecList(): List<UrlSpec> {
  return emptyList()
}
