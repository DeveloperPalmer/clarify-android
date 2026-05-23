package ru.sla.clarify.lib.google.firestore.codec.sentinel

/**
 * Sentinel-маркер для Firestore-поля типа «временная метка с сервера». На write
 * подменяется на `FieldValue.serverTimestamp()` через [ServerTimestampSerializer].
 *
 * Использование в *Params:
 * ```
 * @Contextual
 * val updatedAt: ServerTimestamp = ServerTimestamp
 * ```
 */
data object ServerTimestamp
