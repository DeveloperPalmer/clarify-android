package ru.sla.clarify.lib.google.firestore.codec.sentinel

/**
 * Sentinel-маркер атомарного удаления элементов из массива-поля. Подменяется на
 * `FieldValue.arrayRemove(...)` через [ArrayRemoveSerializer] на write.
 *
 * Пустой `values` запрещён: атомарный no-op не имеет смысла и съедает round-trip;
 * скорее всего вызов без элементов — баг на коллсайте.
 *
 * Использование в *Params:
 * ```
 * @Contextual
 * val memberUids: ArrayRemove = ArrayRemove(listOf(uid))
 * ```
 */
data class ArrayRemove(val values: List<String>) {
  init {
    require(values.isNotEmpty()) { "ArrayRemove with no elements is a no-op — likely a bug at call site" }
  }
}
