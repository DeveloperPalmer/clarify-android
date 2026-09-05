package ru.sla.clarify.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Событие кадра до второго шага разбора: номер уже прочитан, само событие — ещё нет.
 *
 * [event] остаётся [JsonElement] намеренно. Разбери мы его вместе с кадром, первый же незнакомый
 * тип уронил бы кадр целиком, вместе со всеми знакомыми событиями в нём, — а сервер обновляется
 * раньше клиента, и незнакомый тип для нас норма, а не край.
 */
@Serializable
internal data class RawSequencedEvent(
  @SerialName("seq")
  val seq: Long,
  @SerialName("event")
  val event: JsonElement
)
