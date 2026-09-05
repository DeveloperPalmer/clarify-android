package ru.sla.clarify.app.data

import kotlinx.serialization.json.Json

/**
 * Один `Json` на весь транспорт: им разбираются и тела операций, и кадры канала, и тела ошибок.
 *
 * `ignoreUnknownKeys` включён не для удобства, а потому что сервер обновляется раньше клиента:
 * новое поле в ответе — норма, и ронять на нём разбор нельзя.
 */
internal val ServerJson: Json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
}
