package ru.kode.log

interface LogDriver {
  /**
   * Logs message using provided priority and tag
   */
  fun log(priority: LogPriority, tag: String, message: String)

  /**
   * Determines if messages with [priority] should be logged.
   * If driver returns `false` here, then message's lambdas in log functions won't be evaluated by the [Logger]
   */
  fun isLoggable(priority: LogPriority): PriorityLogging
}

enum class PriorityLogging {
  Enabled,
  Disabled
}
