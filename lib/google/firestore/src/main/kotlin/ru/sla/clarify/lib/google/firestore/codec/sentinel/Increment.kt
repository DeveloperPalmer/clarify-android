package ru.sla.clarify.lib.google.firestore.codec.sentinel

/**
 * Sentinel-маркер атомарного инкремента числового поля. Подменяется на
 * `FieldValue.increment(value)` через [IncrementSerializer] на write.
 *
 * `value == 0` запрещён: атомарный no-op не имеет смысла и съедает round-trip;
 * скорее всего вызов с нулём — баг на коллсайте.
 *
 * Использование в *Params:
 * ```
 * @Contextual
 * val count: Increment = Increment(1)
 * ```
 */
data class Increment(val value: Long) {
  init {
    require(value != 0L) { "Increment by 0 is a no-op — likely a bug at call site" }
  }
}
