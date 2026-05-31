package ru.sla.clarify.lib.google.firestore

import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult

/**
 * Элемент snapshot-listener'а: тип изменения и payload документа. Используется
 * только для **стримов** (`*Live`-методы) — у read-once `get*` методов нет понятия
 * "change type", они возвращают обьект ответа напрямую.
 */
data class FirestoreChange<T>(
  val changeType: FirestoreDocumentResult,
  val data: T,
  val hasPendingWrites: Boolean = false
)
