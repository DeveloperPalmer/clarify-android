package ru.sla.clarify.core.domain.entity

interface LceStateGeneric<out C : Any, out E : Any> {
  val content: C?
  val isContent: Boolean
  fun asContent(): C

  val error: E?
  val isError: Boolean
  fun asError(): E

  val isLoading: Boolean
}
