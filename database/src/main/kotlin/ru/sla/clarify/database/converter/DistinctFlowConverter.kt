package ru.sla.clarify.database.converter

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import ru.sla.clarify.database.DistinctFlow

/**
 * Повторяет встроенный `Flow` Room: подписка на таблицы запроса, первый выпуск сразу, промежуточные
 * инвалидации схлопываются — и отдаёт результат как [DistinctFlow].
 */
object DistinctFlowConverter {
  // Таблиц в запросе единицы, а копия массива делается раз на подписку, а не на выпуск
  @Suppress("SpreadOperator")
  @DaoReturnTypeConverter([OperationType.READ])
  fun <T> convert(
    database: RoomDatabase,
    tableNames: List<String>,
    executeAndConvert: suspend () -> T
  ): DistinctFlow<T> {
    val upstream = database.invalidationTracker
      .createFlow(*tableNames.toTypedArray(), emitInitialState = true)
      .conflate()
      .map { executeAndConvert() }
    return DistinctFlow(upstream)
  }
}
