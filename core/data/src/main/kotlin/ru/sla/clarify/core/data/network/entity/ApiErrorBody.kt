package ru.sla.clarify.core.data.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorBody(
  val code: String,
  val message: String
)
