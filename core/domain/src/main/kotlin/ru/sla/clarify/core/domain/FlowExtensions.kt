package ru.sla.clarify.core.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import ru.kode.remo.JobFlow
import ru.kode.remo.JobState
import ru.sla.clarify.core.domain.entity.LceState

fun <T, S> Flow<T>.mapDistinctChanges(transform: suspend (T) -> S): Flow<S> {
  return this.map(transform).distinctUntilChanged()
}

fun <T, S : Any> Flow<T>.mapDistinctNotNullChanges(transform: suspend (T) -> S?): Flow<S> {
  return this.mapNotNull(transform).distinctUntilChanged()
}

/**
 * A stream of loading states and task execution results in the form of LCE states. If [replayLastResult] is
 * set to `false`, then only execution results (Content/Error) _after_ subscription will be returned,
 * otherwise, the last result (if any) will be sent immediately.
 */
fun <T> JobFlow<T>.asLceState(replayLastResult: Boolean = false): Flow<LceState<Unit>> {
  return channelFlow {
    launch {
      this@asLceState.results(replayLast = replayLastResult)
        .collect {
          when (it) {
            is Ok -> {
              send(LceState.Content(Unit))
            }
            is Err -> send(LceState.Error(it.error))
          }
        }
    }
    launch {
      this@asLceState.state.collect {
        when (it) {
          JobState.Idle -> Unit // relying on result() emissions to report idle
          JobState.Running -> send(LceState.Loading)
        }
      }
    }
  }
}
