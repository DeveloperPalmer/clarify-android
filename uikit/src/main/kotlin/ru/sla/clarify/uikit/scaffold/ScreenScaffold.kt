package ru.sla.clarify.uikit.scaffold

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.uikit.components.ErrorSnackbar
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.compose.resolveTextRef

@Composable
fun ScreenScaffold(
  // NOTE (!) avoid modifying argument list unless it's needed for at least 5 screens.
  // Prefer copying implementation until it will be proved that your modification
  // is repeated many times
  state: ScreenScaffoldState = rememberScreenScaffoldState(),
  onDismissSnackbarError: () -> Unit,
  onDismissDialogError: (DialogDismissReason) -> Unit,
  contentLoadPlaceholder: @Composable () -> Unit = { ScreenScaffold.ContentLoadProgressIndicator() },
  contentLoadError: @Composable (errorState: ContentLoadState.Error) -> Unit = { errorState ->
    ScreenScaffold.ContentLoadError(errorState)
  },
  contentReady: @Composable () -> Unit
  // NOTE (!) avoid modifying argument list unless it's needed for at least 5 screens.
  // Prefer copying implementation until it will be proved that your modification
  // is repeated many times
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AppTheme.colors.bgPrimary)
  ) {
    when (val contentLoadState = state.contentLoadState) {
      is ContentLoadState.Error -> {
        contentLoadError(contentLoadState)
      }

      is ContentLoadState.Loading -> {
        contentLoadPlaceholder()
      }

      is ContentLoadState.NotStarted -> Unit
      is ContentLoadState.Ready -> {
        contentReady()
      }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarHost(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding(),
      hostState = snackbarHostState
    ) { data ->
      ErrorSnackbar(data)
    }
    val snackbarErrorMessage = state.snackbarError
      ?.message
      ?.let { message ->
        val title = resolveTextRef(source = message.title).trim()
        val description = message.description?.let { resolveTextRef(source = it) }
        description ?: title
      }
    LaunchedEffect(snackbarErrorMessage) {
      if (snackbarErrorMessage != null) {
        when (snackbarHostState.showSnackbar(message = snackbarErrorMessage, duration = SnackbarDuration.Short)) {
          SnackbarResult.Dismissed -> onDismissSnackbarError()
          SnackbarResult.ActionPerformed -> Unit
        }
      }
    }

    val dialogError = state.dialogError
    if (dialogError != null) {
      AlertDialog(
        onDismissRequest = { onDismissDialogError(DialogDismissReason.OutsideClick) },
        title = {
          Text(
            style = AppTheme.typography.button,
            textAlign = TextAlign.Center,
            text = resolveTextRef(dialogError.message.title)
          )
        },
        confirmButton = {
          if (dialogError.message.primaryAction != null) {
            val action = dialogError.message.primaryAction!!
            Button(
              onClick = {
                onDismissDialogError(DialogDismissReason.PrimaryButtonClick)
                action.listener()
              }
            ) {
              Text(
                style = AppTheme.typography.button,
                textAlign = TextAlign.Center,
                text = resolveTextRef(action.name)
              )
            }
          } else {
            Button(onClick = { onDismissDialogError(DialogDismissReason.DismissButtonClick) }) {
              Text(
                style = AppTheme.typography.button,
                textAlign = TextAlign.Center,
                text = "Ok"
              )
            }
          }
        },
        // when there's a primary action we show this action + cancel-button otherwise we show only OK button
        dismissButton = @Suppress("UseLet") if (dialogError.message.primaryAction != null) {
          {
            Button(
              onClick = { onDismissDialogError(DialogDismissReason.DismissButtonClick) }
            ) {
              Text(
                style = AppTheme.typography.button,
                textAlign = TextAlign.Center,
                text = "Cancel"
              )
            }
          }
        } else {
          null
        },
        text = {
          Text(
            text = dialogError.message.description?.let { resolveTextRef(it) } ?: "Error"
          )
        }
      )
    }
  }
}

object ScreenScaffold {
  @Composable
  fun ContentLoadProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  }

  // NOTE: AVOID adding new overloads unless they repeat at least 5 times!
  // Prefer adding custom composables local to the screen and simply pass them to scaffold as composable lambdas,
  // do it until you notice something repeats often, only then add a new overload

  @Composable
  fun ContentLoadError(errorState: ContentLoadState.Error) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val error = errorState.error
      Text(
        style = AppTheme.typography.h2,
        textAlign = TextAlign.Center,
        text = resolveTextRef(source = error.message.title)
      )
      VSpacer(size = 8.dp)
      if (error.message.description != null) {
        Text(
          style = AppTheme.typography.body2,
          textAlign = TextAlign.Center,
          text = resolveTextRef(source = error.message.description!!)
        )
      }

      if (error.message.primaryAction != null) {
        VSpacer(size = 44.dp)
        Button(onClick = error.message.primaryAction!!.listener) {
          val textAlpha by animateFloatAsState(
            targetValue = if (errorState.refreshInProgress) 0f else 1f
          )
          Text(
            modifier = Modifier.alpha(textAlpha),
            style = AppTheme.typography.button,
            textAlign = TextAlign.Center,
            text = resolveTextRef(source = error.message.primaryAction!!.name)
          )
          AnimatedVisibility(
            visible = errorState.refreshInProgress,
            enter = fadeIn(),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh))
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = AppTheme.colors.textPrimary
            )
          }
        }
      }
    }
  }
}

@Suppress("ConvertSecondaryConstructorToPrimary")
// false positive, conversion is not possible
@Stable
class ScreenScaffoldState {
  var snackbarError: UiError? by mutableStateOf(null)
  var dialogError: UiError? by mutableStateOf(null)
  var contentLoadState: ContentLoadState by mutableStateOf(ContentLoadState.Ready)

  constructor(
    contentLoadState: ContentLoadState = ContentLoadState.Ready,
    snackbarError: UiError? = null,
    dialogError: UiError? = null
  ) {
    this.contentLoadState = contentLoadState
    this.snackbarError = snackbarError
    this.dialogError = dialogError
  }
}

@Composable
fun rememberScreenScaffoldState(
  contentLoadState: ContentLoadState = ContentLoadState.Ready,
  snackbarError: UiError? = null,
  dialogError: UiError? = null
): ScreenScaffoldState {
  return remember {
    ScreenScaffoldState(contentLoadState, snackbarError, dialogError)
  }
}

enum class DialogDismissReason {
  OutsideClick,
  PrimaryButtonClick,
  DismissButtonClick
}
