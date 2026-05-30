package ru.sla.clarify.feature.debug.panel.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.debug.panel.domain.entity.UserJsonError

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val userField: String = "",
  val userSending: Boolean = false,
  val userJsonError: UserJsonError? = null
)
