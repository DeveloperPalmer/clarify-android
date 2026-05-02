package ru.sla.clarify.core.domain.entity

sealed class ConnectivityError : Throwable() {
  abstract override val cause: Throwable?

  data class TimeOut(
    override val cause: Throwable
  ) : ConnectivityError()

  data class NoConnection(
    override val cause: Throwable?
  ) : ConnectivityError()

  data class SSLError(
    override val cause: Throwable?
  ) : ConnectivityError()
}
