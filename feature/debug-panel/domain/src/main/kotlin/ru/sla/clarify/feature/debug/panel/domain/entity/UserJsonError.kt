package ru.sla.clarify.feature.debug.panel.domain.entity

sealed interface UserJsonError {
  data object MalformedJson : UserJsonError
  data class MissingField(val field: String) : UserJsonError
  data class UnknownField(val field: String) : UserJsonError
  data object InvalidPhotoUrl : UserJsonError
  data object UserAlreadyExist : UserJsonError
}

class DebugUserException(val error: UserJsonError) : RuntimeException()
