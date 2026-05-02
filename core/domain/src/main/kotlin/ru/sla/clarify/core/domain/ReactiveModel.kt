package ru.sla.clarify.core.domain

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.log.asLog

open class ReactiveModel : ru.kode.remo.ReactiveModel() {
  override fun onPostStart() {
    super.onPostStart()
    uncaughtExceptions
      .onEach { e ->
        logError { e.asLog("uncaught model exception") }
      }
      .launchIn(scope)
  }
}
