package ru.sla.clarify.core.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Stable
class DropdownMenuAnchorState {
  var offset: DpOffset by mutableStateOf(DpOffset(0.dp, 0.dp))
    private set

  fun set(offset: DpOffset) {
    this.offset = offset
  }
}

val LocalDropdownMenuAnchor = staticCompositionLocalOf<DropdownMenuAnchorState> {
  error(
    "No DropdownMenuAnchorState provided. " +
      "Wrap your composition with CompositionLocalProvider(LocalDropdownMenuAnchor provides ...)."
  )
}

/**
 * Tracks layout coordinates of a component so a click handler can publish a precise anchor
 * (window-space [DpOffset]) into [LocalDropdownMenuAnchor] right before emitting a
 * `ViewEvent.DropdownMenu`.
 *
 * Wire it up with [Modifier.captureDropdownMenuAnchor] on the component that should receive
 * clicks, then call [anchor] from the click handler.
 */
@Stable
class DropdownMenuAnchorScope internal constructor(
  private val anchorState: DropdownMenuAnchorState,
  private val density: Density
) {
  private var layoutCoordinates: LayoutCoordinates? = null

  internal fun updateCoords(coords: LayoutCoordinates) {
    layoutCoordinates = coords
  }

  /**
   * Publishes the tracked component's top-right corner (in window space) as the dropdown anchor.
   *
   * The x is offset by the component's width so that, when Material3's
   * `DropdownMenuPositionProvider` mirrors the menu (because it doesn't fit to the right of the
   * anchor), the menu's right edge aligns with the component's right edge — i.e. the menu opens
   * "over" the component instead of jumping to its left.
   *
   * No-op if the component's layout coordinates have not been captured yet (e.g. when invoked
   * before the first composition pass).
   */
  fun anchor() {
    val coords = layoutCoordinates ?: return
    val relativeToLocal = Offset(
      x = coords.size.width.toFloat(),
      y = 0f
    )
    val windowOffset = coords.localToWindow(relativeToLocal)
    with(density) {
      anchorState.set(
        DpOffset(
          x = windowOffset.x.toDp(),
          y = windowOffset.y.toDp()
        )
      )
    }
  }
}

/**
 * Returns a [DropdownMenuAnchorScope] bound to the current [LocalDropdownMenuAnchor] and
 * [LocalDensity]. Pair the returned scope with [Modifier.captureDropdownMenuAnchor].
 */
@Composable
fun rememberDropdownMenuAnchorScope(): DropdownMenuAnchorScope {
  val anchorState = LocalDropdownMenuAnchor.current
  val density = LocalDensity.current
  return remember(anchorState, density) { DropdownMenuAnchorScope(anchorState, density) }
}

/**
 * Records the modified component's layout coordinates into [scope].
 *
 * Use together with [DropdownMenuAnchorScope.anchor] called from the click handler to publish
 * the component's top-left position to [LocalDropdownMenuAnchor] right before emitting a
 * `ViewEvent.DropdownMenu`.
 */
fun Modifier.captureDropdownMenuAnchor(scope: DropdownMenuAnchorScope): Modifier =
  this.onGloballyPositioned { coords -> scope.updateCoords(coords) }
