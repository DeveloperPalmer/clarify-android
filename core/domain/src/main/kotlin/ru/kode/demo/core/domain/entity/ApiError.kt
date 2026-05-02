package ru.kode.demo.core.domain.entity

data class ApiError(
  val code: String,
  val description: String
) : Throwable()
