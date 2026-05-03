package ru.sla.log.android

import android.util.Log
import ru.sla.log.LogDriver
import ru.sla.log.LogPriority
import ru.sla.log.PriorityLogging

class AndroidLogDriver(
  private val priorityLogging: (priority: LogPriority) -> PriorityLogging
) : LogDriver {

  override fun log(priority: LogPriority, tag: String, message: String) {
    Log.println(priority.toAndroidPriority(), tag, message)
  }

  override fun isLoggable(priority: LogPriority): PriorityLogging {
    return priorityLogging(priority)
  }
}

private fun LogPriority.toAndroidPriority(): Int {
  return when (this) {
    LogPriority.Verbose -> Log.VERBOSE
    LogPriority.Debug -> Log.DEBUG
    LogPriority.Info -> Log.INFO
    LogPriority.Warn -> Log.WARN
    LogPriority.Error -> Log.ERROR
    LogPriority.Assert -> Log.ASSERT
  }
}
