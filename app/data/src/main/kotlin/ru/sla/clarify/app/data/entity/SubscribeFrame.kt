package ru.sla.clarify.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Первый кадр, который клиент шлёт открыв канал: место, с которого просит догон.
 *
 * `lastSeq = 0` — не особый случай, а обычное состояние нового устройства.
 */
@Serializable
internal data class SubscribeFrame(
  @SerialName("lastSeq")
  val lastSeq: Long
)
