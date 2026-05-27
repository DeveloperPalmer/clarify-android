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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.compose.resolveTextRef

@Composable
fun ScreenScaffold(
  // NOTE (!) avoid modifying argument list unless it's needed for at least 5 screens.
  // Prefer copying implementation until it will be proved that your modification
  // is repeated many times
  state: ScreenScaffoldState = rememberScreenScaffoldState(),
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
      .background(AppTheme.colors.backgroundPrimary)
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
        style = AppTheme.typography.headline2,
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
            style = AppTheme.typography.caption,
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
              color = AppTheme.colors.cardPrimary
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
  var contentLoadState: ContentLoadState by mutableStateOf(ContentLoadState.Ready)

  constructor(contentLoadState: ContentLoadState = ContentLoadState.Ready) {
    this.contentLoadState = contentLoadState
  }
}

@Composable
fun rememberScreenScaffoldState(
  contentLoadState: ContentLoadState = ContentLoadState.Ready
): ScreenScaffoldState {
  return remember {
    ScreenScaffoldState(contentLoadState)
  }
}
