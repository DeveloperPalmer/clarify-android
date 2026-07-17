package ru.sla.clarify.core.domain

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import ru.sla.log.asLog

open class ReactiveModel(parentScope: CoroutineScope) : ru.kode.remo.ReactiveModel(parentScope) {
  init {
    uncaughtExceptions
      .onEach { e ->
        logError { e.asLog("uncaught model exception") }
      }
      .launchIn(scope)
  }
}

/**
 * Единая точка создания скоупов для [ReactiveModel]. Владелец скоупа — DI-компонент фичи,
 * завершает его FlowNode при выходе из flow. [parent] встраивает Job в иерархию родительского
 * скоупа, чтобы отмена родителя каскадно гасила дочерние модели.
 */
fun createCoroutineScope(parent: CoroutineScope? = null): CoroutineScope {
  val errorHandler = CoroutineExceptionHandler { _, e ->
    e.printStackTrace()
  }
  return CoroutineScope(SupervisorJob(parent?.coroutineContext?.job) + Dispatchers.Default + errorHandler)
}
