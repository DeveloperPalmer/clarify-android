package ru.sla.clarify.uikit.component.bubble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.component.bubble.BubbleMessage.ReadStatus
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.AppTheme.colors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BubbleMessage(
  bubble: BubbleMessage,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null
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
      .surface(
        shape = shape,
        backgroundColor = backgroundColor,
        onClick = onClick,
        onLongClick = onLongClick
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
      ReadStatus.Sending -> {
        ClockMark(tint = statusMutedColor)
      }
      ReadStatus.Sent -> {
        CheckMark(tint = statusMutedColor)
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

@Composable
private fun ClockMark(
  tint: Color,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "clock")
  val hourAngle by transition.animateFloat(
    label = "hourAngle",
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = CLOCK_HOUR_PERIOD_MILLIS, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )
  val minuteAngle by transition.animateFloat(
    label = "minuteAngle",
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = CLOCK_MINUTE_PERIOD_MILLIS, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )
  Canvas(modifier = modifier.size(checkMarkSize)) {
    val strokeWidth = checkMarkStrokeWidth.toPx()
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = (size.minDimension - strokeWidth) / 2f
    drawCircle(
      color = tint,
      radius = radius,
      center = center,
      style = Stroke(width = strokeWidth)
    )
    drawClockHand(tint, center, radius * 0.5f, hourAngle, strokeWidth)
    drawClockHand(tint, center, radius * 0.82f, minuteAngle, strokeWidth)
  }
}

private fun DrawScope.drawClockHand(
  tint: Color,
  center: Offset,
  length: Float,
  angleDegrees: Float,
  strokeWidth: Float
) {
  val radians = angleDegrees * (PI.toFloat() / HALF_TURN_DEGREES)
  drawLine(
    color = tint,
    start = center,
    end = Offset(
      x = center.x + length * sin(radians),
      y = center.y - length * cos(radians)
    ),
    strokeWidth = strokeWidth,
    cap = StrokeCap.Round
  )
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

private const val CLOCK_HOUR_PERIOD_MILLIS = 5000
private const val CLOCK_MINUTE_PERIOD_MILLIS = 1500

private const val HALF_TURN_DEGREES = 180f
