package ru.sla.clarify.feature.login.entity

sealed interface GoogleAuthError {
  data class CancelledByUser(override val cause: Throwable?) : RuntimeException()
  data class Authentication(override val cause: Throwable?) : RuntimeException()
}
