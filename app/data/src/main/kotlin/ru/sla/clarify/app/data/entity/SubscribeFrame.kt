package ru.sla.clarify.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Первый кадр, который клиент шлёт открыв канал: чем он собран и место, с которого просит догон.
 *
 * `lastSeq = 0` — не особый случай, а обычное состояние нового устройства.
 *
 * [contractVersion] здесь дублирует заголовок рукопожатия намеренно: заголовок доказывает, что
 * копия спеки подходит для команд, а этот кадр — что она подходит для событий, и разъехаться эти
 * два ответа могут только у сервера, который сам себе противоречит.
 */
@Serializable
internal data class SubscribeFrame(
  @SerialName("lastSeq")
  val lastSeq: Long,
  @SerialName("contractVersion")
  val contractVersion: String
)
