package ru.sla.clarify.uikit.component

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.component.button.ButtonStyle
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import kotlin.math.roundToInt

@Composable
fun RevealSplitRow(
  modifier: Modifier = Modifier,
  leftVisible: Boolean = true,
  rightVisible: Boolean = true,
  animationSpec: FiniteAnimationSpec<Float> = AppTheme.motion.mediumTween(),
  gap: Dp = 8.dp,
  left: @Composable () -> Unit,
  right: @Composable () -> Unit
) {
  val leftReveal = animateFloatAsState(
    targetValue = if (leftVisible) 1f else 0f,
    animationSpec = animationSpec,
    label = "reveal-split-row-left"
  )
  val rightReveal = animateFloatAsState(
    targetValue = if (rightVisible) 1f else 0f,
    animationSpec = animationSpec,
    label = "reveal-split-row-right"
  )
  Layout(
    modifier = modifier,
    content = {
      left()
      right()
    }
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    if (measurables.size < 2) {
      return@Layout layout(width, 0) {}
    }
    val leftFraction = leftReveal.value.coerceIn(0f, 1f)
    val rightFraction = rightReveal.value.coerceIn(0f, 1f)
    val gapWidth = (gap.toPx() * leftFraction * rightFraction).roundToInt()
    val available = width - gapWidth
    val leftWidth = (available * leftFraction * (1f - rightFraction / 2f)).roundToInt()
    val rightWidth = (available * rightFraction * (1f - leftFraction / 2f)).roundToInt()

    val leftPlaceable: Placeable
    val rightPlaceable: Placeable
    if (leftFraction >= rightFraction) {
      leftPlaceable = measurables[0].measure(
        constraints.copy(minWidth = leftWidth, maxWidth = leftWidth)
      )
      rightPlaceable = measurables[1].measure(
        Constraints.fixed(width = rightWidth, height = leftPlaceable.height)
      )
    } else {
      rightPlaceable = measurables[1].measure(
        constraints.copy(minWidth = rightWidth, maxWidth = rightWidth)
      )
      leftPlaceable = measurables[0].measure(
        Constraints.fixed(width = leftWidth, height = rightPlaceable.height)
      )
    }
    val height = maxOf(
      if (leftWidth > 0) leftPlaceable.height else 0,
      if (rightWidth > 0) rightPlaceable.height else 0
    )
    layout(width, height) {
      if (leftWidth > 0) {
        leftPlaceable.placeWithLayer(x = 0, y = 0) { alpha = leftFraction }
      }
      if (rightWidth > 0) {
        rightPlaceable.placeWithLayer(x = width - rightWidth, y = 0) { alpha = rightFraction }
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RevealSplitRowPreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    RevealSplitRowPreview()
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RevealSplitRowPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    RevealSplitRowPreview()
  }
}

@Composable
private fun RevealSplitRowPreview() {
  RevealSplitRow(
    left = { PreviewLeftButton() },
    right = { PreviewRightButton() }
  )
  RevealSplitRow(
    rightVisible = false,
    left = { PreviewLeftButton() },
    right = { PreviewRightButton() }
  )
  RevealSplitRow(
    leftVisible = false,
    left = { PreviewLeftButton() },
    right = { PreviewRightButton() }
  )
}

@Composable
private fun PreviewLeftButton() {
  PrimaryButton(
    text = "Left",
    onClick = {}
  )
}

@Composable
private fun PreviewRightButton() {
  PrimaryButton(
    text = "Right",
    style = ButtonStyle.Success,
    onClick = {}
  )
}
