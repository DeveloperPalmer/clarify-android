package ru.sla.clarify.app.android

import ru.sla.clarify.app.android.di.AppComponent
import ru.sla.clarify.app.android.di.AppComponentHolder
import ru.sla.clarify.app.android.di.DaggerAppComponent
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.app.domain.buildconfig.BuildType
import ru.sla.log.LogPriority
import ru.sla.log.Logger
import ru.sla.log.PriorityLogging
import ru.sla.log.android.AndroidLogDriver

class Application : android.app.Application(), AppComponentHolder, BuildConfigProvider {
  private lateinit var _appComponent: AppComponent

  override val buildType: BuildType = when (val type = BuildConfig.BUILD_TYPE) {
    "debug" -> BuildType.Dev
    "internal" -> BuildType.Internal
    "release" -> BuildType.Release
    else -> error("unknown build type name in BuildConfig: $type")
  }

  override fun onCreate() {
    super.onCreate()
    configureLogging()

    _appComponent = buildAppComponent()
  }

  private fun buildAppComponent(): AppComponent {
    return DaggerAppComponent.builder()
      .applicationContext(this)
      .build()
  }

  override val appComponent: AppComponent get() = _appComponent

  private fun configureLogging() {
    val driver = AndroidLogDriver(
      priorityLogging = { priority ->
        when (buildType) {
          BuildType.Dev -> PriorityLogging.Enabled
          BuildType.Internal -> PriorityLogging.Enabled
          BuildType.Release -> if (priority == LogPriority.Error || priority == LogPriority.Assert) {
            PriorityLogging.Enabled
          } else {
            PriorityLogging.Disabled
          }
        }
      }
    )
    Logger.install(driver)
  }
}
