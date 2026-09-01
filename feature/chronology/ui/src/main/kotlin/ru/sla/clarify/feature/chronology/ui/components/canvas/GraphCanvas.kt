package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import ru.sla.atlas.entity.NodeAccent
import ru.sla.atlas.ui.AtlasCanvas
import ru.sla.atlas.ui.AtlasCanvasState
import ru.sla.atlas.ui.CanvasBackdrop
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColors
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Полотно хронологии: атлас, одетый в эту фичу.
 *
 * Само полотно — [AtlasCanvas]: оно держит камеру, ловит жест, ставит узлы и меняет уровни
 * детализации. Здесь к нему добавляется всё, чего библиотека знать не может: палитра тем, кривые из
 * `AppMotion`, узор фона, покраска связей и то, за какую ветку говорит узел.
 *
 * Цвета веток резолвятся здесь и один раз: дальше ни раскладка, ни рёбра, ни мини-карта темы не
 * знают — они получают готовый цвет. Палитра стоит в ключе, потому что смена темы меняет цвета, не
 * трогая граф.
 *
 * @param state камера полотна и результат его последней раскладки
 * @param ceremony церемония слияния: полотно её не запускает, но линия ветки на время церемонии
 *   показывается так, будто ветка снова готова к слиянию
 * @param modifier модификатор корня полотна
 * @param blocked взято ли полотно целиком: пока `true`, жест не начинается вовсе
 * @param node содержимое узла на заданном уровне детализации
 * @param overlay что нарисовать поверх полотна: панель, мини-карта, что угодно
 */
@Composable
internal fun GraphCanvas(
  state: AtlasCanvasState<GraphNode, GraphLevel>,
  ceremony: MergeCeremonyState,
  modifier: Modifier = Modifier,
  blocked: () -> Boolean = { false },
  node: @Composable (graphNode: GraphNode, accent: NodeAccent, level: GraphLevel) -> Unit,
  overlay: @Composable BoxScope.(onBoundsChanged: (key: Any, bounds: Rect) -> Unit) -> Unit = { }
) {
  val colors = AppTheme.colors
  val graph = state.graph
  val branchColors = remember(graph, colors) { graph.toBranchColors(colors) }
  val drawEdges = rememberEdgePainter(edges = state.edges, ceremony = ceremony)
  AtlasCanvas(
    modifier = modifier,
    state = state,
    branchColors = branchColors,
    flingDecay = AppTheme.motion.flingDecay(),
    // Под reduced motion кроссфейда уровней нет вовсе: представление подменяется мгновенно. Гаптику
    // при этом зовёт экран — отклик это не движение, и глушить его вместе с анимацией нельзя (§14).
    crossfadeSpec = if (rememberReducedMotion()) snap() else AppTheme.motion.mediumTween(),
    blocked = blocked,
    // Точка ветвления и точка слияния стоят на магистрали, а показывают ветку, которая от них ушла
    // или в них вернулась. Знать об этом полотну неоткуда: рода узлов — знание фичи.
    accentOwnerOf = { layout, graphNode, own -> layout.accentOwnerOf(graphNode, own) },
    background = {
      CanvasBackdrop(
        modifier = Modifier.fillMaxSize(),
        offset = state.backdropOffset,
        scale = state.backdropScale,
        // Паттерн рисуется `contentPrimary` с очень низкой альфой, а не `cardQuinary`: в тёмной теме
        // `cardQuinary` равен `cardPrimary`, и узлы слились бы с фоном.
        color = colors.contentPrimary.copy(alpha = 0.04f),
        telemetry = state.telemetry
      )
    },
    drawEdges = drawEdges,
    node = node,
    overlay = overlay
  )
}
