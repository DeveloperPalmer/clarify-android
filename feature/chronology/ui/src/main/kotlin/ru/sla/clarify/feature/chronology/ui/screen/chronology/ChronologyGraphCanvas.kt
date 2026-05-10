package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.ui.text.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chronology.domain.ChronologyEdge
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph
import ru.sla.clarify.feature.chronology.domain.ChronologyNode
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import kotlin.math.hypot

private val NODE_WIDTH = 200.dp
private val NODE_HEIGHT = 80.dp
private val H_SPACING = 24.dp
private val V_SPACING = 56.dp
private val CONTENT_PADDING = 32.dp
private const val MIN_SCALE = 0.4f
private const val MAX_SCALE = 3f
private const val ARROW_LENGTH_PX = 14f
private const val ARROW_HALF_WIDTH_PX = 8f
private const val EDGE_STROKE_PX = 3f

@Composable
internal fun ChronologyGraphCanvas(
  graph: ChronologyGraph,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val nodeWidthPx = with(density) { NODE_WIDTH.toPx() }
  val nodeHeightPx = with(density) { NODE_HEIGHT.toPx() }
  val hSpacingPx = with(density) { H_SPACING.toPx() }
  val vSpacingPx = with(density) { V_SPACING.toPx() }
  val contentPaddingPx = with(density) { CONTENT_PADDING.toPx() }

  val layout = remember(graph, nodeWidthPx, nodeHeightPx, hSpacingPx, vSpacingPx) {
    layoutGraph(
      graph = graph,
      nodeWidthPx = nodeWidthPx,
      nodeHeightPx = nodeHeightPx,
      hSpacingPx = hSpacingPx,
      vSpacingPx = vSpacingPx
    )
  }

  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val edgeColor = AppTheme.colors.textPrimary

  Box(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
      .pointerInput(layout) {
        detectTransformGestures { centroid, pan, zoom, _ ->
          val oldScale = scale
          val newScale = (oldScale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
          // Якорим точку под centroid: после зума она должна остаться на том же
          // экранном месте. Точка содержимого, видимая под centroid: p = (centroid - offset) / oldScale.
          // Хотим: centroid + pan = newOffset + newScale * p  →
          // newOffset = centroid + pan - (newScale / oldScale) * (centroid - offset).
          val zoomRatio = if (oldScale == 0f) 1f else newScale / oldScale
          offset = centroid + pan - (centroid - offset) * zoomRatio
          scale = newScale
        }
      }
  ) {
    if (graph.nodes.isEmpty()) {
      ChronologyEmptyHint()
      return@Box
    }

    Box(
      modifier = Modifier
        .graphicsLayer {
          translationX = offset.x
          translationY = offset.y
          scaleX = scale
          scaleY = scale
          transformOrigin = TransformOrigin(0f, 0f)
        }
        .padding(CONTENT_PADDING)
        .size(
          width = with(density) { (layout.contentSize.width).toDp() },
          height = with(density) { (layout.contentSize.height).toDp() }
        )
    ) {
      ChronologyEdges(
        graph = graph,
        layout = layout,
        nodeSize = Size(nodeWidthPx, nodeHeightPx),
        contentPaddingPx = contentPaddingPx,
        edgeColor = edgeColor
      )
      graph.nodes.forEach { node ->
        val position = layout.positions[node.message.id.value] ?: return@forEach
        ChronologyMessageBox(
          node = node,
          modifier = Modifier
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .size(width = NODE_WIDTH, height = NODE_HEIGHT)
        )
      }
    }
  }
}

@Composable
private fun ChronologyEdges(
  graph: ChronologyGraph,
  layout: ChronologyLayout,
  nodeSize: Size,
  contentPaddingPx: Float,
  edgeColor: Color
) {
  // Стрелки рисуются под узлами, размер канвы совпадает с content rect.
  Canvas(
    modifier = Modifier
      .size(
        width = with(LocalDensity.current) { layout.contentSize.width.toDp() },
        height = with(LocalDensity.current) { layout.contentSize.height.toDp() }
      )
  ) {
    graph.edges.forEach { edge ->
      drawEdge(
        edge = edge,
        layout = layout,
        nodeSize = nodeSize,
        color = edgeColor,
        contentPaddingPx = contentPaddingPx
      )
    }
  }
}

private fun DrawScope.drawEdge(
  edge: ChronologyEdge,
  layout: ChronologyLayout,
  nodeSize: Size,
  color: Color,
  contentPaddingPx: Float
) {
  val from = layout.positions[edge.fromMsgId] ?: return
  val to = layout.positions[edge.toMsgId] ?: return
  // Цепочка идёт слева-направо: связь от правой грани родителя к левой грани ребёнка.
  val start = Offset(
    x = from.x + nodeSize.width,
    y = from.y + nodeSize.height / 2f
  )
  val end = Offset(
    x = to.x,
    y = to.y + nodeSize.height / 2f
  )
  // Кривая Безье горизонтально вытянутая, чтобы ребра не перекрывали соседей.
  val controlOffsetX = (end.x - start.x).coerceAtLeast(contentPaddingPx)
  val control1 = Offset(start.x + controlOffsetX / 2f, start.y)
  val control2 = Offset(end.x - controlOffsetX / 2f, end.y)

  val path = Path().apply {
    moveTo(start.x, start.y)
    cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
  }
  drawPath(
    path = path,
    color = color,
    style = Stroke(width = EDGE_STROKE_PX)
  )
  drawArrowHead(
    tip = end,
    fromControl = control2,
    color = color
  )
}

private fun DrawScope.drawArrowHead(
  tip: Offset,
  fromControl: Offset,
  color: Color
) {
  val direction = tip - fromControl
  val length = hypot(direction.x, direction.y)
  if (length == 0f) return
  val unit = Offset(direction.x / length, direction.y / length)
  val normal = Offset(-unit.y, unit.x)
  val base = Offset(tip.x - unit.x * ARROW_LENGTH_PX, tip.y - unit.y * ARROW_LENGTH_PX)
  val left = Offset(base.x + normal.x * ARROW_HALF_WIDTH_PX, base.y + normal.y * ARROW_HALF_WIDTH_PX)
  val right = Offset(base.x - normal.x * ARROW_HALF_WIDTH_PX, base.y - normal.y * ARROW_HALF_WIDTH_PX)
  val path = Path().apply {
    moveTo(tip.x, tip.y)
    lineTo(left.x, left.y)
    lineTo(right.x, right.y)
    close()
  }
  drawPath(path = path, color = color)
}

@Composable
private fun ChronologyMessageBox(
  node: ChronologyNode,
  modifier: Modifier = Modifier
) {
  val message = node.message
  val backgroundColor = if (message.isSelf) {
    AppTheme.colors.backgroundSecondary
  } else {
    AppTheme.colors.textPrimary
  }
  val foregroundColor = if (message.isSelf) {
    AppTheme.colors.textPrimary
  } else {
    AppTheme.colors.backgroundSecondary
  }
  Column(
    modifier = modifier
      .surface(
        shape = AppTheme.shapes.round12,
        backgroundColor = backgroundColor
      )
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = message.previewLabel(),
      style = AppTheme.typography.caption2,
      color = foregroundColor,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = message.text,
      style = AppTheme.typography.body1,
      color = foregroundColor,
      maxLines = 2
    )
  }
}

@Composable
private fun ChronologyEmptyHint() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "Нет сообщений для построения хронологии",
      style = AppTheme.typography.body1,
      color = AppTheme.colors.textPrimary
    )
  }
}

private fun ChatMessage.previewLabel(): String {
  val time = timestamp.format(TIME_FORMATTER_HOUR_MINUTE)
  val author = if (isSelf) "Вы" else senderId
  return "$author • $time"
}
