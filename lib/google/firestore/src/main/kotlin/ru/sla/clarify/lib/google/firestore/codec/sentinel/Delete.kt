package ru.sla.clarify.lib.google.firestore.codec.sentinel

/**
 * Sentinel-маркер «удалить поле из документа» (через `FieldValue.delete()`).
 * Подменяется на `FieldValue.delete()` через [DeleteSerializer] на write.
 *
 * Использование в *Params:
 * ```
 * @Contextual
 * val mergeRequest: Delete = Delete
 * ```
 */
data object Delete
