package ru.kode.log

object Logger {
  @Volatile
  private var driver: LogDriver = NoOpDriver

  /**
   * Log [message] using [priority] and [tag].
   * If [tag] is not provided, then class' simple name will be used.
   *
   * Message's lambda will only be evaluated if currently installed logger determines it as loggable.
   *
   * Use [Throwable.asLog] to log exceptions.
   */
  fun log(priority: LogPriority = LogPriority.Debug, tag: String? = null, message: () -> String) {
    when (driver.isLoggable(priority)) {
      PriorityLogging.Enabled -> driver.log(priority, tag ?: buildDefaultTag(), message())
      PriorityLogging.Disabled -> Unit
    }
  }

  /**
   * Installs a log driver.
   *
   * Installing driver twice without calling [uninstall] in-between will throw [IllegalStateException].
   */
  fun install(driver: LogDriver) {
    synchronized(this) {
      if (this.driver != NoOpDriver) {
        error("Log driver installed more than once. Installing $driver, current is ${this.driver}")
      }
      this.driver = driver
    }
  }

  /**
   * Uninstalls the currently installed logger.
   */
  fun uninstall() {
    synchronized(this) {
      this.driver = NoOpDriver
    }
  }
}

// This was stolen from square/logcat :o)
private fun Any.buildDefaultTag(): String {
  val javaClass = this::class.java
  val fullClassName = javaClass.name
  val outerClassName = fullClassName.substringBefore('$')
  val simplerOuterClassName = outerClassName.substringAfterLast('.')
  return if (simplerOuterClassName.isEmpty()) {
    fullClassName
  } else {
    simplerOuterClassName.removeSuffix("Kt")
  }
}

/**
 * Log [message] using [priority] and [tag].
 * If [tag] is not provided, then class' simple name will be used.
 *
 * Message's lambda will only be evaluated if currently installed logger determines it as loggable.
 *
 * Use [Throwable.asLog] to log exceptions.
 */
fun Any.log(priority: LogPriority = LogPriority.Debug, tag: String? = null, message: () -> String) {
  Logger.log(priority, tag ?: buildDefaultTag(), message)
}

object NoOpDriver : LogDriver {
  override fun log(priority: LogPriority, tag: String, message: String) = Unit
  override fun isLoggable(priority: LogPriority): PriorityLogging = PriorityLogging.Disabled
}
