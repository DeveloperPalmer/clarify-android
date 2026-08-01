package ru.sla.clarify.uikit.component.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.avatar.stableSeedHash
import ru.sla.clarify.uikit.component.chat.Commit.Message.ReadStatus
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.AppTheme.colors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * [onAnchorBounds] reports the bubble's bounds in window coordinates whenever it is (re)positioned.
 * The context menu is a separate overlay hosted outside the list (so LazyColumn recycling can't kill
 * it mid-animation); it uses these bounds to place itself over the bubble. Pass `null` for bubbles
 * that can never anchor the menu.
 */
@Composable
fun Message(
  message: Commit.Message,
  selectionEnabled: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onAnchorBounds: ((IntRect) -> Unit)?,
  modifier: Modifier = Modifier
) {
  val isRight = message.side is Commit.Message.Side.Right

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

  val shape = when (message.shape) {
    Commit.Message.Shape.Top -> topShape(message.side)
    Commit.Message.Shape.Middle -> middleShape(message.side)
    Commit.Message.Shape.Bottom -> bottomShape(message.side)
  }

  val senderLabel = message.sender?.let { sender ->
    SenderLabel(
      name = sender.name,
      color = senderNameColor(sender.id)
    )
  }
  SelectableBubbleContainer(
    modifier = modifier,
    selected = message.selected,
    selectionEnabled = selectionEnabled,
    onClick = onClick,
    onLongClick = onLongClick
  ) {
    BubbleMessageLayout(
      modifier = Modifier.padding(
        vertical = 2.dp,
        horizontal = 8.dp
      ),
      side = message.side,
      shape = shape,
      backgroundColor = backgroundColor,
      senderLabel = senderLabel,
      text = message.text,
      textColor = contentColor,
      time = message.time,
      edited = message.edited,
      timeColor = timeColor,
      statusReadColor = statusReadColor,
      onAnchorBounds = onAnchorBounds,
      onClick = onClick.takeIf { !selectionEnabled },
      onLongClick = onLongClick.takeIf { !selectionEnabled }
    )
  }
}

@Composable
private fun SelectableBubbleContainer(
  selectionEnabled: Boolean,
  selected: Boolean,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Row(
    modifier = if (selectionEnabled) {
      modifier.surface(
        shape = RectangleShape,
        backgroundColor = if (selected) colors.backgroundAccentPrimary else Color.Transparent,
        onClick = { onClick?.invoke() },
        onLongClick = { onLongClick?.invoke() }
      )
    } else {
      modifier
    },
    verticalAlignment = Alignment.CenterVertically
  ) {
    AnimatedVisibility(
      visible = selectionEnabled,
      enter = AppTheme.motion.slideInFromStart(),
      exit = AppTheme.motion.slideOutToStart()
    ) {
      // Отступ на самом индикаторе (внутри AnimatedVisibility), а не на контейнере: 8.dp едут
      // вместе с индикатором, поэтому в покое он отстоит на 8.dp от края, но выезд из-за края
      // сохраняется (в свёрнутом состоянии слот занимает 0 и лишнего отступа в строке нет).
      SelectionIndicator(
        modifier = Modifier
          .padding(start = 8.dp)
          .size(24.dp),
        selected = selected
      )
    }
    Box(modifier = Modifier.weight(1f)) {
      content()
    }
  }
}

@Composable
private fun SelectionIndicator(
  selected: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedContent(
    modifier = modifier,
    targetState = selected,
    transitionSpec = AppTheme.motion.mediumTransitionSpec(),
    label = "selectionIndicator"
  ) { isChecked ->
    if (isChecked) {
      Image(
        modifier = Modifier
          .fillMaxSize()
          .background(colors.contentAccentPrimary, CircleShape)
          .padding(selectionCheckPadding),
        painter = painterResource(R.drawable.ic_check_16),
        contentDescription = null,
        colorFilter = ColorFilter.tint(colors.contentAccentSecondary)
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .border(
            width = selectionRingWidth,
            shape = CircleShape,
            color = colors.contentTertiary
          )
      )
    }
  }
}

@Composable
private fun senderNameColor(senderId: UserId): Color {
  val palette = listOf(
    colors.contentAccentPrimary,
    colors.successPrimary,
    colors.contentBlue,
    colors.contentGoldPrimary
  )
  return palette[stableSeedHash(senderId.value).mod(palette.size)]
}

@Composable
private fun Modifier.anchorBounds(onAnchorBounds: (IntRect) -> Unit): Modifier {
  return onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInWindow()
    onAnchorBounds(
      IntRect(
        left = bounds.left.roundToInt(),
        top = bounds.top.roundToInt(),
        right = bounds.right.roundToInt(),
        bottom = bounds.bottom.roundToInt()
      )
    )
  }
}

@Composable
private fun BubbleMessageLayout(
  side: Commit.Message.Side,
  shape: Shape,
  backgroundColor: Color,
  senderLabel: SenderLabel?,
  text: String,
  textColor: Color,
  time: String,
  edited: Boolean,
  timeColor: Color,
  statusReadColor: Color,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  onAnchorBounds: ((IntRect) -> Unit)?,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()
  val textStyle = AppTheme.typography.body1
  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = if (side is Commit.Message.Side.Right) {
      Alignment.CenterEnd
    } else {
      Alignment.CenterStart
    }
  ) {
    // Границы самого пузыря (а не полноширинной строки) — по ним встаёт меню-оверлей, живущий
    // вне списка. Модификатор прямо на BubbleSurface — отдельная обёртка была бы лишней.
    BubbleSurface(
      modifier = Modifier.then(onAnchorBounds?.let { Modifier.anchorBounds(it) } ?: Modifier),
      side = side,
      shape = shape,
      backgroundColor = backgroundColor,
      senderLabel = senderLabel,
      text = text,
      textColor = textColor,
      textStyle = textStyle,
      textMeasurer = textMeasurer,
      time = time,
      timeColor = timeColor,
      edited = edited,
      statusReadColor = statusReadColor,
      onClick = onClick,
      onLongClick = onLongClick
    )
  }
}

@Composable
private fun BubbleSurface(
  side: Commit.Message.Side,
  shape: Shape,
  backgroundColor: Color,
  senderLabel: SenderLabel?,
  text: String,
  textColor: Color,
  textStyle: TextStyle,
  textMeasurer: TextMeasurer,
  time: String,
  timeColor: Color,
  edited: Boolean,
  statusReadColor: Color,
  onClick: (() -> Unit)?,
  onLongClick: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  SubcomposeLayout(
    modifier = modifier
      .widthIn(max = 280.dp)
      .surface(
        shape = shape,
        backgroundColor = backgroundColor,
        onClick = onClick,
        onLongClick = onLongClick
      )
      .animateContentSize(
        animationSpec = AppTheme.motion.mediumTween()
      )
      .padding(
        vertical = 8.dp,
        horizontal = 12.dp
      )
  ) { constraints ->
    val looseConstraints = constraints.copy(
      minWidth = 0,
      minHeight = 0
    )

    val timeStatusPlaceable = subcompose(BubbleSlot.TimeStatus) {
      BubbleTimeStatus(
        time = time,
        edited = edited,
        timeColor = timeColor,
        status = (side as? Commit.Message.Side.Right)?.status,
        statusMutedColor = timeColor,
        statusReadColor = statusReadColor
      )
    }.first().measure(looseConstraints)

    val senderPlaceable = senderLabel?.let { label ->
      subcompose(BubbleSlot.Sender) {
        Text(
          text = label.name,
          style = AppTheme.typography.label3Bold,
          color = label.color,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }.first().measure(looseConstraints)
    }
    val senderHeight = if (senderPlaceable != null) {
      senderPlaceable.height + senderNameSpacing.roundToPx()
    } else {
      0
    }

    val textPlaceable = subcompose(BubbleSlot.Text) {
      Text(
        text = text,
        style = textStyle,
        color = textColor
      )
    }.first().measure(looseConstraints)

    val textLayoutResult = textMeasurer.measure(
      text = text,
      style = textStyle,
      constraints = looseConstraints
    )
    val lastLineRight = textLayoutResult.getLineRight(textLayoutResult.lineCount - 1)
    val sameLineWidth = lastLineRight + timeStatusPadding.toPx() + timeStatusPlaceable.width

    if (sameLineWidth <= constraints.maxWidth.toFloat()) {
      val height = senderHeight + textPlaceable.height
      val width = max(textPlaceable.width.toFloat(), sameLineWidth)
        .roundToInt()
        .coerceAtLeast(senderPlaceable?.width ?: 0)
        .coerceIn(constraints.minWidth, constraints.maxWidth)

      layout(width, height) {
        senderPlaceable?.place(0, 0)
        textPlaceable.place(0, senderHeight)
        timeStatusPlaceable.place(
          x = width - timeStatusPlaceable.width,
          y = height - timeStatusPlaceable.height
        )
      }
    } else {
      val height = senderHeight + textPlaceable.height + timeStatusPlaceable.height
      val width = max(textPlaceable.width, timeStatusPlaceable.width)
        .coerceAtLeast(senderPlaceable?.width ?: 0)
        .coerceIn(
          minimumValue = constraints.minWidth,
          maximumValue = constraints.maxWidth
        )

      layout(width, height) {
        senderPlaceable?.place(0, 0)
        textPlaceable.place(0, senderHeight)
        timeStatusPlaceable.place(
          x = width - timeStatusPlaceable.width,
          y = senderHeight + textPlaceable.height
        )
      }
    }
  }
}

@Composable
private fun BubbleTimeStatus(
  time: String,
  edited: Boolean,
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
    AnimatedVisibility(
      visible = edited,
      enter = fadeIn(AppTheme.motion.mediumTween()),
      exit = fadeOut(AppTheme.motion.mediumTween())
    ) {
      Text(
        modifier = Modifier.padding(end = 2.dp),
        text = stringResource(R.string.chat_message_edited),
        style = AppTheme.typography.caption,
        color = timeColor
      )
    }
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

private fun topShape(side: Commit.Message.Side): Shape {
  val isLeft = side is Commit.Message.Side.Left
  return RoundedCornerShape(
    topStart = bubbleSoftCorner,
    topEnd = bubbleSoftCorner,
    bottomStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    bottomEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private fun middleShape(side: Commit.Message.Side): Shape {
  val isLeft = side is Commit.Message.Side.Left
  return RoundedCornerShape(
    topStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    bottomStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    topEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner,
    bottomEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private fun bottomShape(side: Commit.Message.Side): Shape {
  val isLeft = side is Commit.Message.Side.Left
  return RoundedCornerShape(
    bottomStart = bubbleSoftCorner,
    bottomEnd = bubbleSoftCorner,
    topStart = if (isLeft) bubbleHardCorner else bubbleSoftCorner,
    topEnd = if (isLeft) bubbleSoftCorner else bubbleHardCorner
  )
}

private enum class BubbleSlot {
  Sender,
  Text,
  TimeStatus
}

@Immutable
private data class SenderLabel(
  val name: String,
  val color: Color
)

private val bubbleHardCorner = 5.dp
private val bubbleSoftCorner = 20.dp

private val selectionRingWidth = 1.5.dp
private val selectionCheckPadding = 4.dp

private val timeStatusPadding = 8.dp
private val timeStatusTopPadding = 2.dp
private val senderNameSpacing = 2.dp

private val checkMarkSize = 12.dp
private val checkMarkStrokeWidth = 1.5.dp
private val doubleCheckMarkOffset = 5.dp

private const val CLOCK_HOUR_PERIOD_MILLIS = 5000
private const val CLOCK_MINUTE_PERIOD_MILLIS = 1500

private const val HALF_TURN_DEGREES = 180f
