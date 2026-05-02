package ru.sla.clarify.core.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
data class UiError(
  val message: UiMessage,
  val cause: Throwable? = null
)
