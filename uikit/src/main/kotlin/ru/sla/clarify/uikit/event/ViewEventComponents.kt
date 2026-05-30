package ru.sla.clarify.uikit.event

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import ru.sla.clarify.core.ui.event.LocalDropdownMenuAnchor
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent.Snackbar.Duration
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.uikit.component.button.TextButton
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef

/**
 * Standard snackbar [ViewEvent] presentation.
 *
 * Dismiss happens either automatically by [SnackbarHost] timeout or by calling
 * [ViewEventHostScope.dismissEventPresentation] (e.g. on action click).
 * If you need more customization, implement your own [ViewEvent.Snackbar] and emit it
 * via `sendViewEvent` directly.
 */
@Immutable
data class Snackbar(
  val message: TextRef,
  val actionLabel: TextRef? = null,
  override val isError: Boolean = false,
  override val duration: Duration = Duration(hasActions = actionLabel != null),
  val action: () -> Unit = {}
) : ViewEvent.Snackbar {

  @Composable
  override fun ViewEventHostScope.Content() {
    val backgroundColor = if (isError) {
      AppTheme.colors.errorPrimary
    } else {
      AppTheme.colors.contentPrimary
    }
    val contentColor = if (isError) {
      AppTheme.colors.contentPrimary
    } else {
      AppTheme.colors.cardPrimary
    }
    Box(
      modifier = Modifier
        .systemBarsPadding()
        .padding(horizontal = 16.dp)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter
    ) {
      Snackbar(
        containerColor = backgroundColor,
        contentColor = contentColor,
        action = actionLabel?.let { label ->
          {
            TextButton(
              text = resolveTextRef(label),
              isError = isError,
              onClick = {
                action()
                dismissEventPresentation()
              }
            )
          }
        }
      ) {
        Text(
          text = resolveTextRef(message),
          color = contentColor,
          style = AppTheme.typography.body2
        )
      }
    }
  }
}

@Immutable
data class DropdownMenu(
  val items: List<ViewEvent.DropdownMenu.Item>,
  val properties: PopupProperties = PopupProperties(focusable = true),
  val onDismissRequest: (() -> Unit)? = null
) : ViewEvent.DropdownMenu {

  @Composable
  override fun ViewEventHostScope.Content() {
    BackHandler {
      onDismissRequest?.invoke()
      dismissEventPresentation()
    }
    val anchor = LocalDropdownMenuAnchor.current.offset
    // Position the popup's layout parent at the anchor point, so Material3's
    // DropdownMenuPositionProvider sees a zero-sized anchor at (anchor.x, anchor.y) and places
    // the menu's top-left exactly there (with the standard out-of-screen fallbacks).
    Box(modifier = Modifier.offset(x = anchor.x, y = anchor.y)) {
      DropdownMenu(
        expanded = true,
        properties = properties,
        onDismissRequest = {
          onDismissRequest?.invoke()
          dismissEventPresentation()
        }
      ) {
        items.forEach { item ->
          DropdownMenuItem(
            enabled = item.isEnabled,
            text = {
              Text(
                text = resolveTextRef(item.title),
                style = AppTheme.typography.caption,
                color = if (item.isDestructive) {
                  AppTheme.colors.errorPrimary
                } else {
                  AppTheme.colors.contentPrimary
                }
              )
            },
            onClick = {
              item.onClick()
              dismissEventPresentation()
            }
          )
        }
      }
    }
  }
}

/**
 * Standard dialog [ViewEvent] presentations.
 *
 * Dismiss happens by calling [ViewEventHostScope.dismissEventPresentation] from inside the dialog actions.
 * If you need more customization, implement your own [ViewEvent.Content] subclass.
 */
@Immutable
sealed class Dialog : ViewEvent.Content() {
  abstract val title: TextRef?
  abstract val text: TextRef?

  /**
   * Decision dialog with two buttons. Cancellable by clicking outside or by back press.
   */
  @Immutable
  data class Decision(
    override val title: TextRef? = null,
    override val text: TextRef? = null,
    val primaryActionTitle: TextRef,
    val secondaryActionTitle: TextRef,
    val primaryAction: (() -> Unit)? = null,
    val secondaryAction: (() -> Unit)? = null,
    val onDismissRequest: (() -> Unit)? = null,
    val isDestructive: Boolean = false
  ) : Dialog() {

    @Composable
    override fun ViewEventHostScope.Content() {
      BackHandler {
        onDismissRequest?.invoke()
        dismissEventPresentation()
      }
      AlertDialog(
        containerColor = AppTheme.colors.cardSecondary,
        onDismissRequest = {
          onDismissRequest?.invoke()
          dismissEventPresentation()
        },
        title = title?.let { titleRef ->
          {
            Text(
              text = resolveTextRef(titleRef),
              color = AppTheme.colors.contentPrimary,
              style = AppTheme.typography.headline3
            )
          }
        },
        text = text?.let { textRef ->
          {
            Text(
              text = resolveTextRef(textRef),
              color = AppTheme.colors.contentSecondary,
              style = AppTheme.typography.body2
            )
          }
        },
        confirmButton = {
          TextButton(
            text = resolveTextRef(primaryActionTitle),
            isError = isDestructive,
            onClick = {
              primaryAction?.invoke()
              dismissEventPresentation()
            }
          )
        },
        dismissButton = {
          TextButton(
            text = resolveTextRef(secondaryActionTitle),
            isError = false,
            onClick = {
              secondaryAction?.invoke()
              dismissEventPresentation()
            }
          )
        }
      )
    }
  }

  /**
   * Information dialog with a single button. Cancellable by clicking outside or by back press.
   */
  @Immutable
  data class Info(
    override val title: TextRef,
    val buttonText: TextRef,
    override val text: TextRef? = null,
    val onButtonClick: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
  ) : Dialog() {

    @Composable
    override fun ViewEventHostScope.Content() {
      BackHandler {
        onDismiss?.invoke()
        dismissEventPresentation()
      }
      AlertDialog(
        onDismissRequest = {
          onDismiss?.invoke()
          dismissEventPresentation()
        },
        title = { Text(resolveTextRef(title)) },
        text = text?.let { textRef -> { Text(resolveTextRef(textRef)) } },
        confirmButton = {
          TextButton(
            text = resolveTextRef(buttonText),
            isError = false,
            onClick = {
              onButtonClick?.invoke()
              dismissEventPresentation()
            }
          )
        }
      )
    }
  }

  /**
   * Blocking dialog with a single button. NOT cancellable by clicking outside or by back press.
   * The only way to dismiss is via [onButtonClick].
   */
  @Immutable
  data class Confirm(
    override val title: TextRef,
    override val text: TextRef,
    val buttonText: TextRef,
    val onButtonClick: (() -> Unit)? = null
  ) : Dialog() {

    @Composable
    override fun ViewEventHostScope.Content() {
      // non-cancellable: swallow back press
      BackHandler {}
      AlertDialog(
        onDismissRequest = {
          // non-cancellable, dismiss is intentionally ignored
        },
        title = { Text(resolveTextRef(title)) },
        text = { Text(resolveTextRef(text)) },
        confirmButton = {
          TextButton(
            text = resolveTextRef(buttonText),
            isError = false,
            onClick = {
              onButtonClick?.invoke()
              dismissEventPresentation()
            }
          )
        }
      )
    }
  }
}
