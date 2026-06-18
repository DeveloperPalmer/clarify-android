package ru.sla.clarify.app.android

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kode.way.Back
import ru.kode.way.Event
import ru.kode.way.Ignore
import ru.kode.way.NavigationService
import ru.kode.way.compose.NodeHost
import ru.kode.way.extension.node.hook.NodeHooksSupportExtensionPoint
import ru.kode.way.extension.service.LogTransitionsExtensionPoint
import ru.kode.way.name
import ru.sla.clarify.app.android.debug.DebugPanelNotification
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.app.domain.buildconfig.BuildType
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
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.debug.panel.routing.DebugPanelFlow
import ru.sla.clarify.uikit.animation.LocalSharedTransitionScope
import ru.sla.clarify.uikit.event.ViewEventsHost
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.window.setNavigationBarColorCompat
import ru.sla.log.asLog
import ru.sla.log.log

class MainActivity : ComponentActivity() {
  private val exceptionHandler = CoroutineExceptionHandler { _, error -> log { error.asLog() } }
  private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)

  // Дебаг-панель показывается отдельным оверлей-сервисом поверх основного флоу, см. DebugPanelNotification
  private val isDebugPanelEnabled: Boolean by lazy {
    when ((application!! as BuildConfigProvider).buildType) {
      BuildType.Dev,
      BuildType.Internal -> true
      BuildType.Release -> false
    }
  }
  private val showDebugPanel = mutableStateOf(false)
  private val debugPanelService = mutableStateOf<NavigationService<DebugPanelFlow.Result>?>(null)
  private var debugPanelEventsJob: Job? = null

  private val debugPanelReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == DebugPanelNotification.ACTION_OPEN_DEBUG_PANEL) {
        openDebugPanel()
      }
    }
  }

  private val requestNotificationPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) DebugPanelNotification.install(this)
  }

  private val splashScreenLoading = MutableStateFlow(true)

  @OptIn(ExperimentalAnimationApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    installSplashScreen().setKeepOnScreenCondition { splashScreenLoading.value }

    val appComponent = (applicationContext!! as Application).appComponent
    val conversationRepository = appComponent.conversationRepository()

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

    coroutineScope.launch {
      warmUpApp(conversationRepository)
    }

    setContent {
      val view = LocalView.current
      val isDarkTheme = isSystemInDarkTheme()
      AppTheme(
        currentTheme = if (isDarkTheme) ColorTheme.Dark else ColorTheme.Light
      ) {
        val navigationBarColor = AppTheme.colors.backgroundPrimary
        SideEffect {
          window.setNavigationBarColorCompat(navigationBarColor.toArgb())
          WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
          }
        }
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
          CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            LocalDropdownMenuAnchor provides remember { DropdownMenuAnchorState() },
            LocalViewEventsHostMediator provides component.viewEventsHostMediator()
          ) {
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
            val debugService = debugPanelService.value
            if (showDebugPanel.value && debugService != null) {
              NodeHost(service = debugService)
            }
            ViewEventsHost()
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (!isDebugPanelEnabled) return
    ContextCompat.registerReceiver(
      this,
      debugPanelReceiver,
      IntentFilter(DebugPanelNotification.ACTION_OPEN_DEBUG_PANEL),
      ContextCompat.RECEIVER_NOT_EXPORTED
    )
    ensureDebugNotification()
  }

  override fun onPause() {
    super.onPause()
    if (!isDebugPanelEnabled) return
    unregisterReceiver(debugPanelReceiver)
    DebugPanelNotification.remove(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    coroutineScope.cancel()
  }

  private fun ensureDebugNotification() {
    if (SDK_INT >= TIRAMISU && checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
      requestNotificationPermission.launch(POST_NOTIFICATIONS)
    } else {
      DebugPanelNotification.install(this)
    }
  }

  private fun openDebugPanel() {
    if (showDebugPanel.value) return
    val mediator = FlowEventMediator(coroutineScope)
    val component = (applicationContext!! as Application).appComponent
      .debugPanelFlowComponentBuilder()
      .eventSink(mediator)
      .build()
    val service = NavigationService<DebugPanelFlow.Result>(
      DebugPanelFlow.nodeBuilder(component),
      onFinishRequest = {
        closeDebugPanel()
        Ignore
      }
    )
    service.addNodeExtensionPoint(NodeHooksSupportExtensionPoint())
    debugPanelEventsJob?.cancel()
    debugPanelEventsJob = mediator.events
      .onEach(service::sendEvent)
      .launchIn(coroutineScope)
    debugPanelService.value = service
    showDebugPanel.value = true
  }

  private fun closeDebugPanel() {
    showDebugPanel.value = false
    debugPanelService.value = null
    debugPanelEventsJob?.cancel()
    debugPanelEventsJob = null
  }

  private suspend fun warmUpApp(conversationRepository: ConversationRepository) {
    runSuspendCatching {
      coroutineScope {
        launch { conversationRepository.user.first() }
        launch { conversationRepository.conversations.first() }
      }
      splashScreenLoading.value = false
    }.onFailure {
      splashScreenLoading.value = false
    }
  }
}
