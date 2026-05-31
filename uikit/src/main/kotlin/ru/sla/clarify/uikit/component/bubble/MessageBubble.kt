package ru.sla.clarify.uikit.component.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.component.bubble.BubbleMessage.ReadStatus
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.AppTheme.colors
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BubbleMessage(
  bubble: BubbleMessage,
  modifier: Modifier = Modifier
) {
  val isRight = bubble.side is BubbleMessage.Side.Right

  val backgroundColor by animateColorAsState(
    label = "backgroundColor",
    targetValue = if (isRight) colors.contentAccentPrimary else colors.cardSecondary
  )

  val contentColor by animateColorAsState(
    label = "contentColor",
    targetValue = if (isRight) colors.contentAccentSecondary else colors.contentPrimary
  )

  val timeColor by animateColorAsState(
    label = "timeColor",
    targetValue = if (isRight) colors.contentAccentSecondary else colors.contentTertiary
  )

  val statusReadColor by animateColorAsState(
    label = "statusReadColor",
    targetValue = if (isRight) colors.contentAccentSecondary else timeColor
  )

  val shape = when (bubble.type) {
    BubbleMessage.Type.Top -> topShape(bubble.side)
    BubbleMessage.Type.Middle -> middleShape(bubble.side)
    BubbleMessage.Type.Bottom -> bottomShape(bubble.side)
  }

  BubbleMessageContent(
    modifier = modifier
      .background(
        shape = shape,
        color = backgroundColor
      )
      .padding(
        vertical = 8.dp,
        horizontal = 12.dp
      ),
    text = bubble.text,
    textColor = contentColor
  ) {
    BubbleTimeStatus(
      time = bubble.time,
      timeColor = timeColor,
      status = (bubble.side as? BubbleMessage.Side.Right)?.status,
      statusMutedColor = timeColor,
      statusReadColor = statusReadColor
    )
  }
}

@Composable
private fun BubbleMessageContent(
  text: String,
  textColor: Color,
  modifier: Modifier = Modifier,
  timeStatus: @Composable () -> Unit
) {
  SubcomposeLayout(modifier) { constraints ->
    val looseConstraints = constraints.copy(
      minWidth = 0,
      minHeight = 0
    )

    val timeStatusPlaceable = subcompose(BubbleSlot.TimeStatus, timeStatus)
      .first()
      .measure(looseConstraints)

    var textLayoutResult: TextLayoutResult? = null
    val textPlaceable = subcompose(BubbleSlot.Text) {
      Text(
        text = text,
        style = AppTheme.typography.body1,
        color = textColor,
        onTextLayout = { textLayoutResult = it }
      )
    }.first().measure(looseConstraints)

    val layoutResult = requireNotNull(textLayoutResult)
    val lastLineRight = layoutResult.getLineRight(layoutResult.lineCount - 1)
    val sameLineWidth = lastLineRight + timeStatusPadding.toPx() + timeStatusPlaceable.width
    val fitsSameLine = sameLineWidth <= constraints.maxWidth.toFloat()

    if (fitsSameLine) {
      val height = textPlaceable.height
      val width = max(textPlaceable.width.toFloat(), sameLineWidth)
        .roundToInt()
        .coerceIn(constraints.minWidth, constraints.maxWidth)

      layout(width, height) {
        textPlaceable.place(0, 0)
        timeStatusPlaceable.place(
          x = width - timeStatusPlaceable.width,
          y = height - timeStatusPlaceable.height
        )
      }
    } else {
      val height = textPlaceable.height + timeStatusPlaceable.height
      val width = max(textPlaceable.width, timeStatusPlaceable.width).coerceIn(
        minimumValue = constraints.minWidth,
        maximumValue = constraints.maxWidth
      )

      layout(width, height) {
        textPlaceable.place(0, 0)
        timeStatusPlaceable.place(
          x = width - timeStatusPlaceable.width,
          y = textPlaceable.height
        )
      }
    }
  }
}

@Composable
private fun BubbleTimeStatus(
  time: String,
  timeColor: Color,
  status: ReadStatus?,
  statusMutedColor: Color,
  statusReadColor: Color
) {
  Row(
    modifier = Modifier.padding(top = timeStatusTopPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = time,
      style = AppTheme.typography.caption,
      color = timeColor
    )
    when (status) {
      ReadStatus.Sent -> {
        CheckMark(tint = statusMutedColor)
      }
      ReadStatus.Delivered -> {
        DoubleStatusCheck(tint = statusMutedColor)
      }
      ReadStatus.Read -> {
        DoubleStatusCheck(tint = statusReadColor)
      }
      null -> Unit
    }
  }
}

@Composable
private fun DoubleStatusCheck(
  tint: Color,
  modifier: Modifier = Modifier
) {
  Box(modifier) {
    CheckMark(tint = tint)
    CheckMark(
      tint = tint,
      modifier = Modifier.padding(start = doubleCheckMarkOffset)
    )
  }
}

@Composable
private fun CheckMark(
  tint: Color,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.size(checkMarkSize)) {
    val strokeWidth = checkMarkStrokeWidth.toPx()
    val left = Offset(x = size.width * 0.12f, y = size.height * 0.55f)
    val bottom = Offset(x = size.width * 0.38f, y = size.height * 0.80f)
    val right = Offset(x = size.width * 0.92f, y = size.height * 0.20f)
    drawLine(
      color = tint,
      start = left,
      end = bottom,
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    drawLine(
      color = tint,
      start = bottom,
      end = right,
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
  }
}

private fun topShape(side: BubbleMessage.Side): Shape {
  val isLeft = side is BubbleMessage.Side.Left
  return RoundedCornerShape(
    topStart = bubbleSoftCorner,
    topEnd = bubbleSoftCorner,
    bottomStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    bottomEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private fun middleShape(side: BubbleMessage.Side): Shape {
  val isLeft = side is BubbleMessage.Side.Left
  return RoundedCornerShape(
    topStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    bottomStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    topEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner,
    bottomEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private fun bottomShape(side: BubbleMessage.Side): Shape {
  val isLeft = side is BubbleMessage.Side.Left
  return RoundedCornerShape(
    bottomStart = bubbleSoftCorner,
    bottomEnd = bubbleSoftCorner,
    topStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    topEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private enum class BubbleSlot {
  Text,
  TimeStatus
}

private val bubbleHardCorner = 5.dp
private val bubbleSoftCorner = 20.dp

private val timeStatusPadding = 8.dp
private val timeStatusTopPadding = 2.dp

private val checkMarkSize = 12.dp
private val checkMarkStrokeWidth = 1.5.dp
private val doubleCheckMarkOffset = 5.dp
