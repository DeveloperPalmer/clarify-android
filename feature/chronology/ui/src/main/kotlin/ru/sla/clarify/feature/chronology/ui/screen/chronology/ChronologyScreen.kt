package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chronology.ui.components.canvas.CameraButtons
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphCanvas
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphDebugOverlay
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphMinimap
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberGraphCanvasState
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberMergeCeremonyState
import ru.sla.clarify.feature.chronology.ui.components.canvas.rememberReducedMotion
import ru.sla.clarify.feature.chronology.ui.components.node.EpisodeNode
import ru.sla.clarify.feature.chronology.ui.components.node.ForkNode
import ru.sla.clarify.feature.chronology.ui.components.node.FrontNode
import ru.sla.clarify.feature.chronology.ui.components.node.GlyphNode
import ru.sla.clarify.feature.chronology.ui.components.node.MergeNode
import ru.sla.clarify.feature.chronology.ui.components.node.MergedRequestNode
import ru.sla.clarify.feature.chronology.ui.components.preview.NodePreviewMorph
import ru.sla.clarify.feature.chronology.ui.entity.GraphAnchor
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeSelection
import ru.sla.clarify.feature.chronology.ui.mapper.toBranchColor
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
      val canvasState = rememberGraphCanvasState(
        nodes = state.nodes,
        branches = state.graphBranches
      )
      val ceremonyState = rememberMergeCeremonyState()
      // Какая ветка вернулась в магистраль в этом узле. Точка слияния стоит **на магистрали**, то
      // есть её собственный `branchId` — корневой, и спросить сливающуюся ветку у самого узла
      // нельзя: связь идёт с другой стороны, от `mergedAt` ветки.
      val mergedBranchByNode = remember(state.graphBranches) {
        state.graphBranches.mapNotNull { branch ->
          branch.mergedAt?.let { it to branch.id }
        }.toMap()
      }
      // Церемония доигрывает после того, как композиция чипа уже могла уйти, поэтому scope свой.
      val ceremonyScope = rememberCoroutineScope()
      val haptics = LocalHapticFeedback.current
      val reducedMotion = rememberReducedMotion()
      // Гаптика переключения уровня (§11.3): один отклик на переход. Снимается потоком, а не чтением
      // уровня в теле экрана, — чтение подписало бы весь экран на переключение, а `drop(1)` убирает
      // кадр подписки, иначе отклик приходил бы на открытие экрана. Под reduced motion отклик
      // остаётся: он не движение, и глушить его вместе с анимацией нельзя.
      LaunchedEffect(canvasState) {
        snapshotFlow { canvasState.level.value }
          .drop(1)
          .collect { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }
      }
      // Подпись пузыря мини-карты. Собирается здесь, потому что дату знает экран, а какой узел под
      // центром — полотно; отдаётся `State`, чтобы прочитал её лист, а не тело экрана: чтение
      // прямо тут пересобирало бы лямбды полотна при каждой смене узла под камерой.
      val episodeById = state.episodeById
      val minimapLabel = remember(episodeById, canvasState) {
        derivedStateOf { canvasState.centralNode.value?.let { episodeById[it]?.time } }
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
            val branchName = state.branchNameById[graphNode.branchId]
            val episode = state.episodeById[graphNode.id]
            val description = graphNode.toDescription(episode = episode, branchName = branchName)
            // Род узла решает, что рисовать, и решает здесь, а не в полотне: полотну безразлично,
            // плашка перед ним или круг, — оно ставит по одному элементу на узел.
            when (graphNode.role) {
              GraphNodeRole.Episode -> when (level) {
                // На обзоре от плашки остаётся глиф: читать на этом уровне нечего, по глифам
                // прослеживают форму разговора. Приглушение слитой ветки при этом остаётся —
                // гаснет узел целиком, вместе с гало, ровно как гаснет плашка.
                GraphLevel.Overview -> {
                  val item = state.episodeById.getValue(graphNode.id)
                  GlyphNode(
                    modifier = Modifier.graphicsLayer { alpha = if (item.dim) 0.6f else 1f },
                    color = accent.colorIndex.toBranchColor(AppTheme.colors),
                    contentDescription = description,
                    unreadCount = item.unreadCount
                  )
                }
                GraphLevel.Episodes -> {
                  val item = state.episodeById.getValue(graphNode.id)
                  val preview = state.previewById[graphNode.id]
                  EpisodeNode(
                    // Выбранная плашка гасится, а не убирается: поверхность обязана уехать в карточку,
                    // а не размножиться копией, оставшейся лежать на полотне. Место в раскладке узел
                    // при этом сохраняет — соседи по дорожке не должны шевельнуться.
                    modifier = Modifier.graphicsLayer {
                      alpha = if (graphNode.id == state.selectedNode?.id) 0f else 1f
                    },
                    time = item.time,
                    count = item.count,
                    snippet = item.snippet,
                    contentDescription = description,
                    myShare = item.myShare,
                    unreadCount = item.unreadCount,
                    dim = item.dim,
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
              GraphNodeRole.Fork -> ForkNode(
                laneColor = accent.colorIndex.toBranchColor(AppTheme.colors),
                contentDescription = description
              )
              // Чип «Закрыта» висит с той стороны магистрали, откуда ветка **не** возвращается.
              // Иначе вертикаль возврата прошла бы сквозь него: она стоит на том же X, что и точка
              // слияния, а чип в накопительную ось не входит и перекрыть её не может ничем.
              GraphNodeRole.Merge -> Box(contentAlignment = Alignment.Center) {
                val mergedBranch = mergedBranchByNode[graphNode.id]
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
                    contentDescription = GraphBranchStatus.Merged.toNodeDescription(branchName),
                    modifier = Modifier.layout { measurable, _ ->
                      // Ноль вместо размера: коробку узла задаёт точка слияния, а чип рядом с ней
                      // только рисуется. Мерился бы он вместе с ней — коробка узла стала бы шириной
                      // в чип, а горизонтали дорожки начинаются у её края: линия подходила бы к
                      // точке с пробелом в полчипа с каждой стороны.
                      val placeable = measurable.measure(Constraints())
                      // Родитель выравнивает нулевой размер по центру точки, поэтому смещение на
                      // половину ставит чип центром в центр узла, а отступ уводит его на свою
                      // сторону магистрали.
                      val shift = if (accent.lane < 0) 34.dp.roundToPx() else -34.dp.roundToPx()
                      layout(width = 0, height = 0) {
                        placeable.place(
                          x = -placeable.width / 2,
                          y = -placeable.height / 2 + shift
                        )
                      }
                    },
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
              GraphNodeRole.Front -> FrontNode(
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
                GraphDebugOverlay(
                  state = canvasState,
                  onBoundsChanged = onBoundsChanged
                )
              }
              CameraButtons(
                // Кнопки стоят в той же колонке, что панель и мини-карта, поэтому отступ «над
                // мини-картой» из §11.1 получается сам и не зависит от её высоты числом.
                modifier = Modifier.align(Alignment.End),
                onStart = { canvasState.flyTo(flightScope, flightSpec, GraphAnchor.Start) },
                onFront = { canvasState.flyTo(flightScope, flightSpec, GraphAnchor.Front) },
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
