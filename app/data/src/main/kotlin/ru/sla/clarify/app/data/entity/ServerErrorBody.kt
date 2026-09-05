package ru.sla.clarify.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело ответа с ошибкой, общее для всех операций спеки.
 *
 * Оба поля необязательны намеренно: тело приходит из чужого репозитория, и ответ без `code`
 * (прокси, балансировщик, 502 от инфраструктуры) обязан оставаться разбираемым, а не превращать
 * понятный отказ в ошибку разбора.
 */
@Serializable
internal data class ServerErrorBody(
  @SerialName("code")
  val code: String? = null,
  @SerialName("message")
  val message: String? = null
)
