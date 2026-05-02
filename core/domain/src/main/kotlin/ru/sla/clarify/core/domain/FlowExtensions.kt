package ru.sla.clarify.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

fun <T, S> Flow<T>.mapDistinctChanges(transform: suspend (T) -> S): Flow<S> {
  return this.map(transform).distinctUntilChanged()
}

fun <T, S : Any> Flow<T>.mapDistinctNotNullChanges(transform: suspend (T) -> S?): Flow<S> {
  return this.mapNotNull(transform).distinctUntilChanged()
}
