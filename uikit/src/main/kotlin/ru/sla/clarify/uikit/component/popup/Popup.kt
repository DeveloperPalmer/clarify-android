package ru.sla.clarify.uikit.component.popup

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.strRef

@Composable
internal fun Popup(
  visible: Boolean,
  actions: List<Action>,
  onHidden: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  bottomSafePadding: Dp = 0.dp
) {
  val density = LocalDensity.current
  val popupPositionProvider = remember(density, bottomSafePadding) {
    PopupPositionProvider(
      gap = with(density) { popupAnchorGap.roundToPx() },
      screenInset = with(density) { popupScreenInset.roundToPx() },
      bottomSafe = with(density) { bottomSafePadding.roundToPx() }
    )
  }
  Popup(
    popupPositionProvider = popupPositionProvider,
    properties = PopupProperties(focusable = false),
    onDismissRequest = onDismissRequest
  ) {
    val animationSpec = AppTheme.motion.smallestTween<Float>()
    val revealProgress = remember { Animatable(0f) }
    val currentOnHidden by rememberUpdatedState(onHidden)
    LaunchedEffect(visible) {
      revealProgress.animateTo(
        animationSpec = animationSpec,
        targetValue = if (visible) 1f else 0f
      )
      if (!visible) {
        currentOnHidden()
      }
    }
    val shape = AppTheme.shapes.round16
    val elevation = AppTheme.elevation.small
    Content(
      modifier = modifier.graphicsLayer {
        val progress = revealProgress.value
        alpha = popupAlpha(progress)
        val scale = POPUP_INITIAL_SCALE + (1f - POPUP_INITIAL_SCALE) * progress
        scaleX = scale
        scaleY = scale
        // Shadow rises only after alpha reaches 1 (see popupShadowFraction): a layer with
        // alpha < 1 is composited offscreen where the native elevation shadow is not drawn.
        shadowElevation = elevation.toPx() * popupShadowFraction(progress)
        this.shape = shape
      },
      actions = actions,
      enabled = visible
    )
  }
}

@Composable
private fun Content(
  actions: List<Action>,
  enabled: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .width(256.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round16
      )
  ) {
    actions.forEach { action ->
      Item(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(48.dp),
        action = action,
        enabled = enabled
      )
    }
  }
}

@Composable
private fun Item(
  action: Action,
  enabled: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clickable(
        enabled = enabled,
        indication = null,
        interactionSource = null,
        onClick = action.onClick
      )
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Icon(
      modifier = Modifier.size(22.dp),
      painter = painterResource(action.iconRes),
      tint = AppTheme.colors.contentPrimary,
      contentDescription = null
    )
    Text(
      text = resolveTextRef(action.text),
      style = AppTheme.typography.body1,
      color = AppTheme.colors.contentPrimary
    )
  }
}

private class PopupPositionProvider(
  private val gap: Int,
  private val screenInset: Int,
  private val bottomSafe: Int
) : PopupPositionProvider {

  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize
  ): IntOffset {
    return IntOffset(
      x = popupX(anchorBounds, windowSize, popupContentSize.width),
      y = popupY(anchorBounds, windowSize, popupContentSize.height)
    )
  }

  private fun popupX(anchor: IntRect, window: IntSize, popupWidth: Int): Int {
    val anchorInRightHalf = anchor.center.x > window.width / 2
    val alignedX = if (anchorInRightHalf) anchor.right - popupWidth else anchor.left
    val maxX = maxOf(screenInset, window.width - popupWidth - screenInset)
    return alignedX.coerceIn(screenInset, maxX)
  }

  private fun popupY(anchor: IntRect, window: IntSize, popupHeight: Int): Int {
    val below = anchor.bottom + gap
    val fitsBelow = below + popupHeight <= window.height - bottomSafe
    return if (fitsBelow) {
      below
    } else {
      (anchor.top - gap - popupHeight).coerceAtLeast(screenInset)
    }
  }
}

@Preview
@Composable
private fun PopupPreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    PopupPreview()
  }
}

@Preview
@Composable
private fun PopupPreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    PopupPreview()
  }
}

@Composable
private fun PopupPreview() {
  Content(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(16.dp)
      .shadow(AppTheme.elevation.small, AppTheme.shapes.round16),
    enabled = true,
    actions = listOf(
      Action(
        iconRes = R.drawable.ic_git_fork_24,
        text = strRef("Создать ветку"),
        onClick = {}
      ),
      Action(
        iconRes = R.drawable.ic_copy_24,
        text = strRef("Копировать"),
        onClick = {}
      ),
      Action(
        iconRes = R.drawable.ic_select_24,
        text = strRef("Выбрать"),
        onClick = {}
      ),
      Action(
        iconRes = R.drawable.ic_trash_24,
        text = strRef("Удалить"),
        onClick = {}
      )
    )
  )
}

private fun popupAlpha(progress: Float): Float {
  return (progress / POPUP_FADE_END_FRACTION).coerceAtMost(1f)
}

private fun popupShadowFraction(progress: Float): Float {
  return ((progress - POPUP_FADE_END_FRACTION) / (1f - POPUP_FADE_END_FRACTION)).coerceIn(0f, 1f)
}

@Immutable
data class Action(
  val iconRes: Int,
  val text: TextRef,
  val onClick: () -> Unit
)

private const val POPUP_INITIAL_SCALE = 0.90f
private const val POPUP_FADE_END_FRACTION = 0.4f
private val popupAnchorGap = 10.dp
private val popupScreenInset = 14.dp
