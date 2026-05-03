package ru.sla.clarify.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.log.asLog
import ru.kode.log.log
import ru.kode.way.Back
import ru.kode.way.Event
import ru.kode.way.Ignore
import ru.kode.way.NavigationService
import ru.kode.way.compose.NodeHost
import ru.kode.way.extension.node.hook.NodeHooksSupportExtensionPoint
import ru.kode.way.extension.service.LogTransitionsExtensionPoint
import ru.sla.clarify.app.routing.AppFlow
import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.core.routing.FlowEventMediator
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

class MainActivity : ComponentActivity() {
  private val exceptionHandler = CoroutineExceptionHandler { _, error -> log { error.asLog() } }
  private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)

  @OptIn(ExperimentalAnimationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val appComponent = (applicationContext!! as Application).appComponent

    val flowEventMediator = FlowEventMediator(coroutineScope)
    val component: AppFlowComponent = appComponent.appFlowComponentBuilder()
      .eventSink(flowEventMediator)
      .activity(this)
      .build()

    val service = NavigationService<Unit>(AppFlow.schema, AppFlow.nodeBuilder(component), onFinishRequest = {
      log { "appFlow has finished -> calling activity finish" }
      finish()
      Ignore
    })
    flowEventMediator.events.onEach(service::sendEvent).launchIn(coroutineScope)
    service.addNodeExtensionPoint(NodeHooksSupportExtensionPoint())
    service.addServiceExtensionPoint(LogTransitionsExtensionPoint(logger = { msg -> log(message = msg) }))
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(enabled = true) {
      override fun handleOnBackPressed() {
        flowEventMediator.sendEvent(Event.Back)
      }
    })

    setContent {
      AppTheme(
        currentTheme = ColorTheme.Light
      ) {
        NodeHost(service = service)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    coroutineScope.cancel()
  }
}
