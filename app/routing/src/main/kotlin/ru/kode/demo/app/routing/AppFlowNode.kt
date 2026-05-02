package ru.kode.demo.app.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kode.demo.core.routing.FlowNodeCoroutineScopeHook
import ru.kode.log.log
import ru.kode.way.Finish
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import javax.inject.Inject

class AppFlowNode @Inject constructor() : BaseFlowNode<Unit>() {
  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook<Unit>()

  override val dismissResult = Unit
  override val initial: Target = Target.appFlow.mainFlow(onFinishRequest = { Finish(Unit) })

  override fun onEntry() {
    scope.launch {
      delay(200L)
      log { "scope usage example, you can start domain models here" }
    }
  }
}
