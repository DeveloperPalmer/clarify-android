package ru.sla.clarify.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Кадр канала, каким он приходит с сервера, до разбора самих событий.
 *
 * [resyncFromSeq] приходит вместе с [resyncRequired] и говорит, с какого места идёт лента
 * сейчас. Без него догон после полной выборки некуда было бы продолжить: клиент подписался бы
 * снова со старым номером и получил бы тот же отказ — и так по кругу.
 *
 * Форма кадра — допущение: спеки ещё нет. Сверить, когда серверная команда её напишет.
 */
@Serializable
internal data class ServerFrame(
  @SerialName("resyncRequired")
  val resyncRequired: Boolean = false,
  @SerialName("resyncFromSeq")
  val resyncFromSeq: Long? = null,
  @SerialName("events")
  val events: List<RawSequencedEvent> = emptyList()
)
