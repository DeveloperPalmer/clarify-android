package ru.kode.demo.app.android

import ru.kode.demo.app.android.di.AppComponent
import ru.kode.demo.app.android.di.AppComponentHolder
import ru.kode.demo.app.android.di.DaggerAppComponent
import ru.kode.demo.app.domain.buildconfig.BuildConfigProvider
import ru.kode.demo.app.domain.buildconfig.BuildType
import ru.kode.log.LogPriority
import ru.kode.log.Logger
import ru.kode.log.PriorityLogging
import ru.kode.log.android.AndroidLogDriver

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
          } else PriorityLogging.Disabled
        }
      }
    )
    Logger.install(driver)
  }
}
