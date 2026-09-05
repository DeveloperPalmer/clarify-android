package ru.sla.clarify.app.data.mapper

import ru.kode.pathfinder.Environment

/**
 * Адрес канала из адреса стенда: та же машина, другая схема.
 *
 * @param path путь канала на сервере.
 */
internal fun Environment.toWebSocketUrl(path: String): String {
  val origin = when {
    baseUrl.startsWith("https://") -> baseUrl.replaceFirst("https://", "wss://")
    baseUrl.startsWith("http://") -> baseUrl.replaceFirst("http://", "ws://")
    // Схему стенду задаём мы сами, так что сюда попадает только опечатка в ServerEnvironments
    else -> error("environment baseUrl has no http scheme: $baseUrl")
  }
  return origin.trimEnd('/') + path
}
