package ru.sla.clarify.uikit.component.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
internal fun AnchoredCommitMenu(
  visible: Boolean,
  actions: List<CommitMenuAction>,
  onDismiss: () -> Unit,
  onHidden: () -> Unit,
  modifier: Modifier = Modifier,
  bottomSafePadding: Dp = 0.dp
) {
  val density = LocalDensity.current
  val popupPositionProvider = remember(density, bottomSafePadding) {
    CommitMenuPositionProvider(
      gap = with(density) { menuAnchorGap.roundToPx() },
      screenInset = with(density) { menuScreenInset.roundToPx() },
      bottomSafe = with(density) { bottomSafePadding.roundToPx() }
    )
  }
  Popup(
    popupPositionProvider = popupPositionProvider,
    properties = PopupProperties(focusable = false),
    onDismissRequest = onDismiss
  ) {
    CommitMenu(
      modifier = modifier,
      visible = visible,
      onHidden = onHidden,
      actions = actions
    )
  }
}

@Composable
private fun CommitMenu(
  visible: Boolean,
  actions: List<CommitMenuAction>,
  onHidden: () -> Unit,
  modifier: Modifier = Modifier
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
  CommitMenuCard(
    modifier = modifier.graphicsLayer {
      val progress = revealProgress.value
      alpha = menuAlpha(progress)
      val scale = MENU_INITIAL_SCALE + (1f - MENU_INITIAL_SCALE) * progress
      scaleX = scale
      scaleY = scale
    },
    actions = actions,
    elevation = AppTheme.elevation.small * menuShadowFraction(revealProgress.value),
    clicksEnabled = visible
  )
}

@Composable
private fun CommitMenuCard(
  actions: List<CommitMenuAction>,
  elevation: Dp,
  clicksEnabled: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .width(256.dp)
      .surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round16,
        elevation = elevation
      )
  ) {
    actions.forEach { action ->
      MenuItem(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(48.dp),
        action = action,
        enabled = clicksEnabled
      )
    }
  }
}

@Composable
private fun MenuItem(
  action: CommitMenuAction,
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
      text = action.label,
      style = AppTheme.typography.body1,
      color = AppTheme.colors.contentPrimary
    )
  }
}

@Composable
fun CommitMenuScrim(
  visible: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val animationSpec = AppTheme.motion.smallestTween<Float>()
  val revealProgress = remember { Animatable(0f) }
  val interactionSource = remember { MutableInteractionSource() }
  val dismissOnTap = if (visible) {
    Modifier.clickable(
      interactionSource = interactionSource,
      indication = null,
      onClick = onDismiss
    )
  } else {
    Modifier
  }
  LaunchedEffect(visible) {
    revealProgress.animateTo(
      animationSpec = animationSpec,
      targetValue = if (visible) 1f else 0f
    )
  }
  Box(
    modifier = modifier
      .fillMaxSize()
      .then(dismissOnTap)
      .graphicsLayer {
        alpha = revealProgress.value
      }
      .background(AppTheme.colors.backgroundTertiary.copy(SCRIM_ALPHA))
  )
}

private class CommitMenuPositionProvider(
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
      x = menuX(anchorBounds, windowSize, popupContentSize.width),
      y = menuY(anchorBounds, windowSize, popupContentSize.height)
    )
  }

  private fun menuX(anchor: IntRect, window: IntSize, menuWidth: Int): Int {
    val anchorInRightHalf = anchor.center.x > window.width / 2
    val alignedX = if (anchorInRightHalf) anchor.right - menuWidth else anchor.left
    val maxX = maxOf(screenInset, window.width - menuWidth - screenInset)
    return alignedX.coerceIn(screenInset, maxX)
  }

  private fun menuY(anchor: IntRect, window: IntSize, menuHeight: Int): Int {
    val below = anchor.bottom + gap
    val fitsBelow = below + menuHeight <= window.height - bottomSafe
    return if (fitsBelow) {
      below
    } else {
      (anchor.top - gap - menuHeight).coerceAtLeast(screenInset)
    }
  }
}

@Preview
@Composable
private fun CommitContextMenuPreviewLight() {
  AppTheme(currentTheme = ColorTheme.Light) {
    CommitContextMenuPreviewContent()
  }
}

@Preview
@Composable
private fun CommitContextMenuPreviewDark() {
  AppTheme(currentTheme = ColorTheme.Dark) {
    CommitContextMenuPreviewContent()
  }
}

@Composable
private fun CommitContextMenuPreviewContent() {
  CommitMenuCard(
    modifier = Modifier
      .background(AppTheme.colors.backgroundPrimary)
      .padding(16.dp),
    elevation = AppTheme.elevation.small,
    clicksEnabled = true,
    actions = listOf(
      CommitMenuAction(
        iconRes = R.drawable.ic_git_fork_24,
        label = "Создать ветку",
        onClick = {}
      ),
      CommitMenuAction(
        iconRes = R.drawable.ic_copy_24,
        label = "Копировать",
        onClick = {}
      ),
      CommitMenuAction(
        iconRes = R.drawable.ic_select_24,
        label = "Выбрать",
        onClick = {}
      ),
      CommitMenuAction(
        iconRes = R.drawable.ic_trash_24,
        label = "Удалить",
        onClick = {}
      )
    )
  )
}

private fun menuAlpha(progress: Float): Float {
  return (progress / MENU_FADE_END_FRACTION).coerceAtMost(1f)
}

private fun menuShadowFraction(progress: Float): Float {
  return ((progress - MENU_FADE_END_FRACTION) / (1f - MENU_FADE_END_FRACTION)).coerceIn(0f, 1f)
}

@Immutable
data class CommitMenuAction(
  val iconRes: Int,
  val label: String,
  val onClick: () -> Unit
)

private const val MENU_INITIAL_SCALE = 0.90f
private const val MENU_FADE_END_FRACTION = 0.4f
private val menuAnchorGap = 10.dp
private val menuScreenInset = 14.dp
private const val SCRIM_ALPHA = 0.24f
