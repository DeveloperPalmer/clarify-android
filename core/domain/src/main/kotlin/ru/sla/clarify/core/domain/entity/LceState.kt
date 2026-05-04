package ru.sla.clarify.core.domain.entity

/**
 * Specialization of the LceStateGeneric class for the domain layer.
 * In most projects this will be Throwable, but there may be exceptions
 */
sealed class LceState<out T : Any> : LceStateGeneric<T, Throwable> {
  object Loading : LceState<Nothing>()
  data class Content<C : Any>(val value: C) : LceState<C>()
  data class Error(val value: Throwable) : LceState<Nothing>()

  override val content: T?
    get() = (this as? Content<T>)?.value

  override val isContent: Boolean
    get() = this is Content

  override fun asContent(): T {
    return (this as Content).value
  }

  override val error: Throwable?
    get() = (this as? Error)?.value

  override val isError: Boolean
    get() = this is Error

  override fun asError(): Throwable {
    return (this as Error).value
  }

  override val isLoading: Boolean
    get() = this is Loading
}
