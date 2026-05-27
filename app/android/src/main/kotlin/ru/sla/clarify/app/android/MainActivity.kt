package ru.sla.clarify.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.way.Back
import ru.kode.way.Event
import ru.kode.way.Ignore
import ru.kode.way.NavigationService
import ru.kode.way.compose.NodeHost
import ru.kode.way.extension.node.hook.NodeHooksSupportExtensionPoint
import ru.kode.way.extension.service.LogTransitionsExtensionPoint
import ru.kode.way.name
import ru.sla.clarify.app.routing.AppFlow
import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.core.routing.FlowEventMediator
import ru.sla.clarify.core.routing.noTransition
import ru.sla.clarify.core.routing.popTransition
import ru.sla.clarify.core.routing.pushTransition
import ru.sla.clarify.core.routing.rememberTransitionSpec
import ru.sla.clarify.core.ui.event.DropdownMenuAnchorState
import ru.sla.clarify.core.ui.event.LocalDropdownMenuAnchor
import ru.sla.clarify.core.ui.event.LocalViewEventsHostMediator
import ru.sla.clarify.uikit.event.ViewEventsHost
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.log.asLog
import ru.sla.log.log

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

    val service = NavigationService<Unit>(AppFlow.nodeBuilder(component), onFinishRequest = {
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
      val view = LocalView.current
      val isDarkTheme = isSystemInDarkTheme()
      SideEffect {
        WindowCompat.getInsetsController(window, view).run {
          isAppearanceLightStatusBars = !isDarkTheme
          isAppearanceLightNavigationBars = !isDarkTheme
        }
      }
      AppTheme(
        currentTheme = if (isDarkTheme) ColorTheme.Dark else ColorTheme.Light
      ) {
        CompositionLocalProvider(
          LocalDropdownMenuAnchor provides remember { DropdownMenuAnchorState() },
          LocalViewEventsHostMediator provides component.viewEventsHostMediator()
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            NodeHost(
              service = service,
              transitionSpec = rememberTransitionSpec {
                // TODO @dz @Way this is a rather bad way to go. Should not rely on a hardcoded string which could
                //  unexpectedly change in the flow, which is in different module.
                //
                //  Instead this should either be something like
                //   if (initialState.path == AbsoluteTargets.appFlow.initialFlowResolve.path) { ... }
                //   (after AbsoluteTargets is implemented in Way)
                //
                // or something like
                //
                //  if (initialState.node.findParentFlowNode() is TransitionResolver &&
                //    initialState.node.findParentFlowNode().customTransition(from, to) != null) {
                //    initialState.node.findParentFlowNode().customTransition(from, to)
                //  } else { pushTransition() }
                val fromInitialResolve = initialState?.path?.segments
                  ?.lastOrNull()
                  ?.name == "initialFlowResolve"
                val toLogin = targetState?.path?.segments
                  ?.any { it.name == "loginFlow" } == true
                when {
                  fromInitialResolve -> noTransition()
                  toLogin -> popTransition()
                  else -> pushTransition()
                }
              }
            )
            ViewEventsHost()
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    coroutineScope.cancel()
  }
}
