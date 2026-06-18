package ru.sla.clarify.lib.google.firestore.codec.sentinel

/**
 * Sentinel-маркер атомарного добавления элементов в массив-поле. Подменяется на
 * `FieldValue.arrayUnion(...)` через [ArrayUnionSerializer] на write.
 *
 * Пустой `values` запрещён: атомарный no-op не имеет смысла и съедает round-trip;
 * скорее всего вызов без элементов — баг на коллсайте.
 *
 * Использование в *Params:
 * ```
 * @Contextual
 * val memberUids: ArrayUnion = ArrayUnion(listOf(uid))
 * ```
 */
data class ArrayUnion(val values: List<String>) {
  init {
    require(values.isNotEmpty()) { "ArrayUnion with no elements is a no-op — likely a bug at call site" }
  }
}
