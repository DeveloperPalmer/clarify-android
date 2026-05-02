package ru.kode.demo.core.domain

import ru.kode.log.LogPriority
import ru.kode.log.log
import kotlin.contracts.contract

// Error logging is done quite often, it's worth it to have a shortcut function for this.
// This shouldn't be done for all priorities, just use log() function with priority!
fun Any.logError(tag: String? = null, message: () -> String) {
  log(LogPriority.Error, tag, message)
}

fun Any.checkOrLogError(condition: Boolean, tag: String? = null, message: () -> String) {
  contract {
    returns() implies condition
  }
  if (!condition) {
    logError(tag, message)
  }
}
