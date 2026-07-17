package ru.sla.clarify.app.android

import android.app.Application
import ru.sla.clarify.app.android.di.AppComponent
import ru.sla.clarify.app.android.di.AppComponentHolder
import ru.sla.clarify.app.android.di.create
import ru.sla.clarify.app.domain.buildconfig.BuildConfigProvider
import ru.sla.clarify.app.domain.buildconfig.BuildType
import ru.sla.log.LogPriority
import ru.sla.log.Logger
import ru.sla.log.PriorityLogging
import ru.sla.log.android.AndroidLogDriver

class Application : Application(), AppComponentHolder, BuildConfigProvider {

  override val appComponent: AppComponent = AppComponent::class.create(this)

  override val buildType: BuildType = when (val type = BuildConfig.BUILD_TYPE) {
    "debug" -> BuildType.Dev
    "internal" -> BuildType.Internal
    "release" -> BuildType.Release
    else -> error("unknown build type name in BuildConfig: $type")
  }

  override fun onCreate() {
    super.onCreate()
    configureLogging()
  }

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
