package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.atlas.debug.DebugOverlay
import ru.sla.atlas.entity.Anchor
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.ui.drawnOnly
import ru.sla.atlas.ui.rememberAtlasCanvasState
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.CameraButtons
import ru.sla.clarify.feature.chronology.ui.components.canvas.ChronologyLevels
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphMinimap
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberMergeCeremonyState
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberReducedMotion
import ru.sla.clarify.feature.chronology.ui.components.node.EpisodeNode
import ru.sla.clarify.feature.chronology.ui.components.node.ForkNode
import ru.sla.clarify.feature.chronology.ui.components.node.FrontNode
import ru.sla.clarify.feature.chronology.ui.components.node.GlyphNode
import ru.sla.clarify.feature.chronology.ui.components.node.MergeNode
import ru.sla.clarify.feature.chronology.ui.components.node.MergedRequestNode
import ru.sla.clarify.feature.chronology.ui.components.preview.NodePreviewMorph
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeSelection
import ru.sla.clarify.feature.chronology.ui.entity.Node
import ru.sla.clarify.feature.chronology.ui.mapper.toDebugLabel
import ru.sla.clarify.feature.chronology.ui.mapper.toDescription
import ru.sla.clarify.feature.chronology.ui.mapper.toNodeDescription
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.component.scrim.ScrimEffect
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChronologyScreen(viewModel: ChronologyViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    // Открыта карточка — закрывается она; иначе закрывается экран, и этим занимается уже хост флоу.
    BackHandler(
      enabled = state.selectedNode != null,
      onBack = intents.closeNodePreview
    )
    ScreenScaffold(state = scaffoldState) {
      val canvasState = rememberAtlasCanvasState(
        graph = state.graph.layout,
        levels = ChronologyLevels,
        initialLevel = GraphLevel.Episodes
      )
      val ceremonyState = rememberMergeCeremonyState()
      // Церемония доигрывает после того, как композиция чипа уже могла уйти, поэтому scope свой.
      val ceremonyScope = rememberCoroutineScope()
      val haptics = LocalHapticFeedback.current
      val reducedMotion = rememberReducedMotion()
      // Подпись пузыря мини-карты. Собирается здесь, потому что дату знает экран, а какой узел под
      // центром — полотно; отдаётся `State`, чтобы прочитал её лист, а не тело экрана: чтение
      // прямо тут пересобирало бы лямбды полотна при каждой смене узла под камерой.
      val minimapLabel = remember(canvasState) {
        // Время есть только у эпизода: над точкой на линии пузырь остаётся без подписи, а не берёт
        // её у соседа.
        derivedStateOf { (canvasState.centralNode.value as? Node.Episode)?.time }
      }
      // Перелёт доигрывает после того, как кнопка могла уйти с экрана вместе с панелью, поэтому
      // scope берётся у экрана, а не у неё. Кривая — из темы: 400 мс, затухание без разгона.
      val flightScope = rememberCoroutineScope()
      val flightSpec = AppTheme.motion.largeTween<Float>()
      Box(modifier = Modifier.fillMaxSize()) {
        // Скругление плашки берётся из того же токена, которым она нарисована, и умножается на
        // масштаб: на 2.5× нарисованный радиус равен сорока, и морф, стартовавший с шестнадцати,
        // начался бы с чужими углами.
        val density = LocalDensity.current
        val episodeCorner = with(density) {
          AppTheme.shapes.round16.topStart.toPx(Size.Zero, this).toDp()
        }
        GraphCanvas(
          modifier = Modifier.fillMaxSize(),
          state = canvasState,
          ceremony = ceremonyState,
          // Пока карточка открыта, полотно жестов не берёт вовсе: прямоугольник, из которого вырос
          // морф, заморожен, и уехавшая под скримом камера сделала бы обратный морф ложью.
          blocked = { state.selectedNode != null },
          node = { graphNode, accent, level ->
            // Имя ветки в узел не приходит: узлу оно не нужно ни для чего, кроме подписи, а подпись
            // собирается здесь — там, где имя вообще есть. У магистрали имени нет, и подпись про
            // ветку тогда не произносится вовсе.
            val branchName = state.graph.branchNames[state.graph.layout.branchOf(graphNode.id).id]
            val description = graphNode.toDescription(branchName = branchName)
            // Род узла решает, что рисовать, и решает здесь, а не в полотне: полотну безразлично,
            // плашка перед ним или круг, — оно ставит по одному элементу на узел.
            when (graphNode) {
              is Node.Episode -> when (level) {
                // На обзоре от плашки остаётся глиф: читать на этом уровне нечего, по глифам
                // прослеживают форму разговора. Приглушение слитой ветки при этом остаётся —
                // гаснет узел целиком, вместе с гало, ровно как гаснет плашка.
                GraphLevel.Overview -> {
                  GlyphNode(
                    modifier = Modifier.graphicsLayer { alpha = if (graphNode.dim) 0.6f else 1f },
                    color = accent.color,
                    contentDescription = description,
                    unreadCount = graphNode.unreadCount
                  )
                }
                GraphLevel.Episodes -> {
                  val preview = state.graph.previewById[graphNode.id]
                  EpisodeNode(
                    // Выбранная плашка гасится, а не убирается: поверхность обязана уехать в карточку,
                    // а не размножиться копией, оставшейся лежать на полотне. Место в раскладке узел
                    // при этом сохраняет — соседи по дорожке не должны шевельнуться.
                    modifier = Modifier.graphicsLayer {
                      alpha = if (graphNode.id == state.selectedNode?.id) 0f else 1f
                    },
                    time = graphNode.time,
                    count = graphNode.count,
                    snippet = graphNode.snippet,
                    contentDescription = description,
                    myShare = graphNode.myShare,
                    unreadCount = graphNode.unreadCount,
                    dim = graphNode.dim,
                    // Узел без содержимого карточки не нажимается вовсе: кнопка, которой некуда
                    // вести, хуже её отсутствия.
                    onClick = preview?.let { content ->
                      {
                        // Прямоугольник снимается в момент тапа и дальше не пересчитывается: камера
                        // под открытой карточкой стоит.
                        canvasState.nodeRectOf(graphNode.id)?.let { bounds ->
                          intents.selectNode(
                            GraphNodeSelection(
                              id = graphNode.id,
                              preview = content,
                              bounds = bounds,
                              corner = episodeCorner * canvasState.scale.value
                            )
                          )
                        }
                      }
                    }
                  )
                }
              }
              // Точка ветвления показывает **уходящую** ветку, поэтому цвет берётся из акцента, а не
              // из самого узла: сам он стоит на магистрали. Направления узел не показывает — его
              // показывает ребро ухода, а время на графе всегда идёт слева направо.
              is Node.Fork -> ForkNode(
                laneColor = accent.color,
                contentDescription = description
              )
              // Чип «Закрыта» висит с той стороны магистрали, откуда ветка **не** возвращается.
              // Иначе вертикаль возврата прошла бы сквозь него: она стоит на том же X, что и точка
              // слияния, а чип в накопительную ось не входит и перекрыть её не может ничем.
              is Node.Merge -> Box(contentAlignment = Alignment.Center) {
                // Какая ветка вернулась в магистраль в этом узле. Точка слияния стоит **на
                // магистрали**, то есть её собственный `branchId` — корневой, и спросить
                // сливающуюся ветку у самого узла нельзя: связь идёт с другой стороны, от
                // `mergedAt` ветки.
                val mergedBranch = state.graph.layout.branchMergedAt(graphNode.id)
                MergeNode(
                  contentDescription = description,
                  // Кадр читается лямбдой, то есть в фазе рисования: значение, взятое здесь,
                  // рекомпоновало бы узел внутри раскладки полотна на каждом кадре церемонии.
                  ceremony = {
                    ceremonyState.frame.value
                      ?.takeIf { ceremonyState.branch.value == mergedBranch }
                  }
                )
                // На обзоре чипа нет: §5 не числит его в скелете смысла, а его 87 dp на посадке
                // превращаются в две сотни пикселей экрана. Смещение ±34 dp при шаге дорожки 26 dp
                // увело бы его вдобавок на чужую дорожку.
                if (level == GraphLevel.Episodes) {
                  MergedRequestNode(
                    // Подпись у чипа своя: он говорит «эта тема закрыта», а точка под ним — «здесь
                    // ветка вернулась в магистраль». Одной фразой на двоих это не сказать.
                    contentDescription = Branch.Status.Merged.toNodeDescription(branchName),
                    // Чип в коробку узла не входит: мерился бы он вместе с точкой слияния — коробка
                    // стала бы шириной в чип, а горизонтали дорожки начинаются у её края, и линия
                    // подходила бы к точке с пробелом в полчипа с каждой стороны. Отступ уводит чип
                    // на ту сторону магистрали, откуда ветка **не** возвращается.
                    modifier = Modifier
                      .offset(y = if (accent.lane < 0) 34.dp else (-34).dp)
                      .drawnOnly(),
                    onClick = mergedBranch?.let { branchId ->
                      {
                        ceremonyState.play(
                          scope = ceremonyScope,
                          branchId = branchId,
                          reduced = reducedMotion,
                          // Первая тактильная отдача в проекте. `Confirm`, а не `LongPress`: удар
                          // кадра 5 означает «дело доведено до конца», и это ровно семантика
                          // константы, а не сила вибрации.
                          onImpact = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                          }
                        )
                      }
                    }
                  )
                }
              }
              is Node.Front -> FrontNode(
                contentDescription = description,
                hasCaption = level == GraphLevel.Episodes
              )
            }
          },
          overlay = { onBoundsChanged ->
            // Инструменты стоят колонкой у нижнего края, мини-карта снизу: колонка растёт вверх,
            // поэтому появление панели не двигает мини-карту, а §11.1 требует, чтобы полоса стояла
            // на месте всегда.
            Column(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              if (state.debugOverlayAvailable && state.debugOverlayVisible) {
                DebugOverlay(
                  state = canvasState,
                  levelLabelOf = { level -> level.toDebugLabel() },
                  onBoundsChanged = onBoundsChanged
                )
              }
              CameraButtons(
                // Кнопки стоят в той же колонке, что панель и мини-карта, поэтому отступ «над
                // мини-картой» из §11.1 получается сам и не зависит от её высоты числом.
                modifier = Modifier.align(Alignment.End),
                onStart = { canvasState.flyTo(flightScope, flightSpec, Anchor.Start) },
                onFront = { canvasState.flyTo(flightScope, flightSpec, Anchor.Front) },
                onBoundsChanged = onBoundsChanged
              )
              GraphMinimap(
                span = canvasState.viewportSpan,
                marks = canvasState.laneMarks,
                label = minimapLabel,
                onScrub = { fraction -> canvasState.scrubTo(fraction) },
                onBoundsChanged = onBoundsChanged
              )
            }
          }
        )
        ChronologyToolbar(
          modifier = Modifier.padding(horizontal = 4.dp),
          debugOverlayVisible = state.debugOverlayVisible,
          debugOverlayAvailable = state.debugOverlayAvailable,
          onBack = intents.navigateBack,
          onToggleDebugOverlay = intents.toggleDebugOverlay
        )
        // Скрим накрывает и шапку: иначе стрелка «назад» осталась бы живой и уводила бы с экрана
        // вместо того, чтобы закрыть карточку.
        ScrimEffect(
          visible = state.selectedNode != null,
          onFinish = intents.closeNodePreview
        )
        // Обе половины морфа стоят в том же Box, что и полотно: прямоугольник якоря считан в его
        // системе координат, и любой другой родитель сдвинул бы его на своё смещение.
        NodePreviewMorph(selection = state.selectedNode)
      }
    }
  }
}

@Composable
private fun ChronologyToolbar(
  debugOverlayVisible: Boolean,
  debugOverlayAvailable: Boolean,
  onBack: () -> Unit,
  onToggleDebugOverlay: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    TopAppBarDefaults.NavigationIcon(
      modifier = Modifier.align(Alignment.CenterStart),
      onClick = onBack
    )
    Text(
      modifier = Modifier.align(Alignment.Center),
      text = stringResource(R.string.chronology_placeholder),
      style = AppTheme.typography.title1Bold,
      color = AppTheme.colors.contentPrimary
    )
    if (debugOverlayAvailable) {
      IconAction(
        modifier = Modifier.align(Alignment.CenterEnd),
        iconResId = R.drawable.ic_debug_24,
        iconTint = if (debugOverlayVisible) {
          AppTheme.colors.contentPrimary
        } else {
          AppTheme.colors.contentTertiary
        },
        onClick = onToggleDebugOverlay
      )
    }
  }
}
