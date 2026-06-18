package ru.sla.clarify.database.extension

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observe a SQLDelight query as a list, ALWAYS deduplicating consecutive identical results.
 *
 * **Why this exists.** SQLDelight emits on every write to any of the query's referenced tables —
 * including writes that produce the same row set. If such an emission is piped into:
 *   - `flatMapLatest { remoteListener(...) }` it tears down and re-creates the remote listener
 *     on every irrelevant write. With Firestore that means a new initial fetch (= billable reads)
 *     on each tear-down. Real-world incident: this exact pattern burned a daily Firestore quota
 *     in minutes.
 *   - a Compose `state.copy(list = ...)` it triggers recomposition for every irrelevant write,
 *     which can grow `SnapshotIdSet` to OOM under load.
 *
 * Using this helper instead of the raw `asFlow().mapToList(...)` removes both classes of bugs
 * at the type level — there is no API surface that returns a non-deduped Flow.
 *
 * If a caller really needs the un-deduped stream (rare; usually for diagnostics), call the
 * underlying SQLDelight `asFlow()` + `mapToList()` directly. Doing so should be code-reviewed.
 */
fun <T : Any> Query<T>.observeList(
  context: CoroutineDispatcher = Dispatchers.IO
): Flow<List<T>> {
  return asFlow()
    .mapToList(context)
    .distinctUntilChanged()
}

/**
 * Observe a SQLDelight query as a nullable single result, with the same deduplication contract
 * as [observeList]. Use for queries that return at most one row.
 */
fun <T : Any> Query<T>.observeOneOrNull(
  context: CoroutineDispatcher = Dispatchers.IO
): Flow<T?> {
  return asFlow()
    .mapToOneOrNull(context)
    .distinctUntilChanged()
}

/**
 * Observe a SQLDelight query as a single non-null result. Throws when the query produces no row.
 * Same deduplication contract as [observeList].
 */
fun <T : Any> Query<T>.observeOne(
  context: CoroutineDispatcher = Dispatchers.IO
): Flow<T> {
  return asFlow()
    .mapToOne(context)
    .distinctUntilChanged()
}
