package ru.sla.clarify.core.ui.entity

import androidx.compose.runtime.Immutable

/**
 * A state which represents a loading progress of a whole screen content.
 * Used to render progress/error of *initial* screen load state.
 * This is not a state to represent progress/error state of subsequent screen actions.
 *
 * Examples:
 *  - loading of user profile: uses ContentLoadState
 *  - reacting to user pressing Login button: **should not** use ContentLoadState,
 *    because this is an action when content is already displayed.
 *    Progress/error is rendered differently in this case (usually)
 */
@Immutable
sealed interface ContentLoadState {
  data object NotStarted : ContentLoadState
  data object Loading : ContentLoadState
  data object Ready : ContentLoadState

  @Immutable
  data class Error(val error: UiError, val refreshInProgress: Boolean) : ContentLoadState

  companion object
}
