package ru.sla.clarify.core.routing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ru.kode.way.Event
import ru.kode.way.FlowTransition
import ru.kode.way.extension.node.hook.BaseScreenNode
import ru.kode.way.extension.node.hook.FlowNodeHook
import ru.kode.way.extension.node.hook.ScreenNodeHook
import kotlin.reflect.KProperty

class FlowNodeScopeDisposalHook<R : Any>(private val scope: CoroutineScope) : FlowNodeHook<R> {
  override fun onPreEntry() = Unit
  override fun onPostEntry() = Unit
  override fun onPreExit() = Unit
  override fun onPostExit() {
    scope.cancel()
  }
  override fun onPreTransition(event: Event) = Unit
  override fun onPostTransition(event: Event, transition: FlowTransition<R>) = Unit
  override fun onPostDispose() {
    scope.cancel()
  }
}

class ScreenNodeCoroutineScopeHook(dispatcher: CoroutineDispatcher = Dispatchers.Default) : ScreenNodeHook {
  private val errorHandler = CoroutineExceptionHandler { _, e ->
    e.printStackTrace()
  }
  private val scope = CoroutineScope(SupervisorJob() + dispatcher + errorHandler)

  operator fun getValue(thisRef: BaseScreenNode, property: KProperty<*>): CoroutineScope {
    thisRef.addHook(this)
    return scope
  }

  override fun onPreEntry() = Unit
  override fun onPostEntry() = Unit
  override fun onPreExit() = Unit
  override fun onPostExit() {
    scope.cancel()
  }
}
