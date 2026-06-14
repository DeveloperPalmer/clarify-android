package ru.sla.clarify.core.ui.event

import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import ru.sla.resourcerefs.TextRef

/**
 * One time UI event presentation, requested by screens or flows and handled by
 * [ru.sla.clarify.uikit.event.ViewEventsHost]. Standard UI kit implementations
 * can be found in [ru.sla.clarify.uikit.event].
 */
@Immutable
sealed interface ViewEvent {

  /**
   * Provides a means to override presentation of a given view event, if the standard
   * presentations from [ru.sla.clarify.uikit.event] do not fit your needs.
   */
  @Composable
  fun ViewEventHostScope.Content()

  @Immutable
  interface Snackbar : ViewEvent {
    val duration: Duration
    val isError: Boolean

    @Immutable
    data class Duration(
      val duration: SnackbarDuration = SnackbarDuration.Short,
      // presence of action could affect duration by means of accessibility manager, see [SnackbarHost]
      val hasActions: Boolean = false
    )
  }

  /**
   * Anchor-less popup menu rendered by [ru.sla.clarify.core.ui.event.DropdownMenuHost].
   *
   * Since the menu is presented from the root host (i.e. has no Composable anchor in the
   * caller's tree), concrete implementations are expected to provide their own positioning
   * via the underlying Material3 `DropdownMenu` `offset` parameter inside [Content].
   * Dismiss happens by calling [ViewEventHostScope.dismissEventPresentation] from inside
   * menu item actions or from `onDismissRequest`.
   */
  @Immutable
  interface DropdownMenu : ViewEvent {
    @Immutable
    data class Item(
      val title: TextRef,
      val onClick: () -> Unit,
      val isEnabled: Boolean = true,
      val isDestructive: Boolean = false
    )
  }

  @Immutable
  abstract class Content : ViewEvent {
    open val animation: Animation = Animation.Fade
    open fun postDestroy() {}
  }

  @Immutable
  interface BottomSheet : ViewEvent {
    val sheetState: State<SheetState?>
  }

  @Immutable
  data object None : ViewEvent {
    @Composable
    override fun ViewEventHostScope.Content() {
    }
  }

  enum class Animation {
    Fade,
    Slide
  }
}

@Stable
fun interface ViewEventHostScope {
  fun dismissEventPresentation()
}
