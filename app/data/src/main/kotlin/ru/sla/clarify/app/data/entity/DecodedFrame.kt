package ru.sla.clarify.app.data.entity

/**
 * Кадр после разбора: знакомые события, и признак того, что догон невозможен.
 *
 * [skippedTypes] — типы, которых клиент не знает. Их не выбрасывают молча: по ним видно, что
 * сервер ушёл вперёд, и именно они подскажут, когда обновлять копию спеки.
 *
 * [firstSeq] и [lastSeq] считаются по **сырому** кадру, включая пропущенные события. Считай их
 * по разобранным — пропущенное незнакомое событие выглядело бы разрывом нумерации, и клиент
 * потребовал бы полной выборки ровно там, где всё в порядке.
 */
data class DecodedFrame<out E>(
  val resyncRequired: Boolean,
  val resyncFromSeq: Long?,
  val events: List<SequencedEvent<E>>,
  val skippedTypes: List<String>,
  val firstSeq: Long?,
  val lastSeq: Long?
)
