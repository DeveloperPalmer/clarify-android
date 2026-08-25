package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.GraphLaneMark
import ru.sla.clarify.feature.chronology.ui.entity.GraphViewportSpan
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneColor
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppColors
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import kotlin.math.roundToInt

/**
 * Мини-карта полотна: вся история сжато, рамка видимого и засечки веток (§11.1 брифа).
 *
 * Это **карта, а не полоса прокрутки**. Мир полосы — границы содержимого, и он не зависит от
 * масштаба: засечка ветки стоит на одном месте и при 0.4×, и при 2.5×. Скроллбарная арифметика,
 * где рамка ходит по остатку хода, дружила бы с минимальной шириной лучше, но у неё нет общей
 * системы координат с засечками — а именно движение «тащу рамку к цветной засечке, чтобы попасть в
 * ветку» и есть то, ради чего мини-карта существует.
 *
 * Скраб ловится по **всей полосе**, а не только по рамке: честная рамка занимает 2.5 % полосы при
 * 2.5×, и требование «попади сначала в рамку» вернуло бы ровно ту беду, ради которой заведён
 * минимальный размер хвата. Ни инерции, ни анимации у скраба нет: бросок означал бы пролёт истории
 * после отпускания, а анимация рассинхронизировала бы рамку с пальцем на следующем же кадре.
 *
 * Компонент принимает всё готовым и не считает ничего, кроме перевода пикселей полосы в доли:
 * долю видимого считает камера, дату — экран. Поэтому появление настоящих данных вместо демо-набора
 * его не заденет.
 *
 * @param span какая доля содержимого по времени видна сейчас; читается в фазе рисования
 * @param marks засечки веток в долях содержимого
 * @param label подпись пузыря — дата узла под центром экрана; читается в листе
 * @param onScrub доля содержимого, которую просят вывести в центр экрана
 * @param onBoundsChanged куда встала полоса и когда её не стало: полотно по этой зоне отличает
 *   палец на мини-карте от пальца на графе
 * @param modifier модификатор мини-карты
 */
@Composable
internal fun GraphMinimap(
  span: State<GraphViewportSpan>,
  marks: State<List<GraphLaneMark>>,
  label: State<String?>,
  onScrub: (fraction: Float) -> Unit,
  onBoundsChanged: (key: Any, bounds: Rect) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AppTheme.colors
  val description = stringResource(R.string.chronology_minimap)
  var isScrubbing by remember { mutableStateOf(false) }
  var trackWidth by remember { mutableIntStateOf(0) }
  val currentOnScrub by rememberUpdatedState(onScrub)
  val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)
  DisposableEffect(Unit) {
    onDispose { currentOnBoundsChanged(ZONE_KEY, Rect.Zero) }
  }

  // Позиция для скринридера округляется до процента намеренно. Семантика пересобирается при каждом
  // изменении того, что прочитано в её блоке, а доля видимого меняется на каждом кадре
  // панорамирования — без округления полотно инвалидировало бы семантику покадрово всю дорогу.
  val position = remember(span) { derivedStateOf { (span.value.position * 100f).roundToInt() / 100f } }

  Box(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        // 48 dp — высота полосы из §11.1 брифа.
        .height(48.dp)
        .onGloballyPositioned {
          trackWidth = it.size.width
          currentOnBoundsChanged(ZONE_KEY, it.boundsInRoot())
        }
        .surface(
          backgroundColor = colors.cardPrimary,
          shape = AppTheme.shapes.round12,
          elevation = AppTheme.elevation.large
        )
        .pointerInput(Unit) {
          detectScrub(
            onScrubbingChanged = { isScrubbing = it },
            onScrub = { x ->
              // Ширина рамки нужна и здесь: палец задаёт середину рамки, а её ход — это полоса за
              // вычетом собственной ширины. Считается из того же состояния и тем же правилом, что
              // и при рисовании, иначе палец и рамка разъедутся.
              val padding = TRACK_PADDING.toPx()
              val frame = widenedSpanOf(
                span = span.value,
                minWidth = MIN_FRAME_WIDTH.toPx() / (size.width - padding * 2f)
              )
              currentOnScrub(
                scrubbedPositionOf(
                  x = x,
                  width = size.width.toFloat(),
                  padding = padding,
                  frameWidth = frame.width
                )
              )
            }
          )
        }
        .semantics {
          contentDescription = description
          progressBarRangeInfo = ProgressBarRangeInfo(current = position.value, range = 0f..1f)
          setProgress { fraction ->
            currentOnScrub(fraction)
            true
          }
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawMinimap(span = span.value, marks = marks.value, colors = colors)
      }
    }
    AnimatedVisibility(
      // Пузырь висит над полосой и места в раскладке не занимает: появившись, он не должен двигать
      // полосу — та обязана стоять на месте всегда, иначе рамка уезжает из-под пальца в момент
      // начала скраба.
      modifier = Modifier
        .align(Alignment.TopStart)
        .graphicsLayer {
          val padding = TRACK_PADDING.toPx()
          val track = trackWidth - padding * 2f
          val frame = widenedSpanOf(span = span.value, minWidth = MIN_FRAME_WIDTH.toPx() / track)
          val centre = padding + trackCentreOf(frame.position, frame.width) * track
          translationX = (centre - size.width / 2f)
            .coerceIn(0f, (trackWidth - size.width).coerceAtLeast(0f))
          translationY = -size.height - 4.dp.toPx()
        },
      visible = isScrubbing,
      enter = fadeIn(AppTheme.motion.smallTween()),
      exit = fadeOut(AppTheme.motion.smallTween())
    ) {
      MinimapDateBubble(label = label)
    }
  }
}

/**
 * Пузырь с датой того места истории, куда ведёт скраб.
 *
 * Закрывает задачу «перейти к дате» без датапикера, которого в проекте нет.
 *
 * @param label дата под центром экрана; пока её нет, пузыря нет тоже
 * @param modifier модификатор пузыря
 */
@Composable
private fun MinimapDateBubble(
  label: State<String?>,
  modifier: Modifier = Modifier
) {
  val text = label.value
  if (text != null) {
    Box(
      modifier = modifier.surface(
        backgroundColor = AppTheme.colors.cardPrimary,
        shape = AppTheme.shapes.round12,
        elevation = AppTheme.elevation.medium
      )
    ) {
      BasicText(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = AppTheme.typography.caption.copy(color = AppTheme.colors.contentSecondary)
      )
    }
  }
}

/**
 * Полоса целиком: магистраль, засечки веток и рамка видимого.
 *
 * @param span честная доля видимого; расширяется до хвата здесь, потому что минимум задан пальцем,
 *   а не содержимым
 * @param marks засечки веток
 * @param colors палитра активной темы
 */
private fun DrawScope.drawMinimap(
  span: GraphViewportSpan,
  marks: List<GraphLaneMark>,
  colors: AppColors
) {
  val padding = TRACK_PADDING.toPx()
  val track = size.width - padding * 2f
  if (track <= 0f) {
    return
  }
  val middle = size.height / 2f
  drawLine(
    color = colors.contentQuaternary,
    start = Offset(x = padding, y = middle),
    end = Offset(x = size.width - padding, y = middle),
    strokeWidth = 1.dp.toPx()
  )
  // 32 dp — наименьшая рамка, которую можно взять пальцем: честная её ширина падает до 10 dp на
  // самом крупном масштабе демо-набора. Обратный ход, если хват окажется избыточным, — 24 dp.
  val frame = widenedSpanOf(span = span, minWidth = MIN_FRAME_WIDTH.toPx() / track)
  marks.fastForEach { mark ->
    // Засечка размечается тем же ходом, что и рамка, поэтому рамка накрывает её центром ровно
    // тогда, когда ветка оказывается в центре экрана. Ценой того, что при зуме засечки у краёв
    // полосы сдвигаются вместе с шириной рамки — на 15 dp от 0.4× к единице, в середине ни на
    // сколько.
    val x = padding + trackCentreOf(position = mark.position, width = frame.width) * track
    // Ветка выше магистрали — засечка вверх, ниже — вниз. Номер дорожки на полосе показать нечем,
    // а знак читается без подписи и бесплатно.
    val end = if (mark.lane < 0) middle - MARK_HEIGHT.toPx() else middle + MARK_HEIGHT.toPx()
    drawLine(
      color = mark.lane.toLaneColor(colors),
      start = Offset(x = x, y = middle),
      end = Offset(x = x, y = end),
      strokeWidth = 2.dp.toPx()
    )
  }
  val frameWidth = frame.width * track
  val centre = trackCentreOf(position = frame.position, width = frame.width) * track
  val topLeft = Offset(x = padding + centre - frameWidth / 2f, y = FRAME_INSET.toPx())
  val frameSize = Size(
    width = frameWidth,
    height = size.height - FRAME_INSET.toPx() * 2f
  )
  val corner = CornerRadius(8.dp.toPx())
  drawRoundRect(
    color = colors.contentAccentPrimary.copy(alpha = 0.12f),
    topLeft = topLeft,
    size = frameSize,
    cornerRadius = corner
  )
  // Акцент здесь законен: §3.2 брифа запрещает его как цвет мелкого текста на тёмном, а заливки,
  // линии и обводки разрешает прямо.
  drawRoundRect(
    color = colors.contentAccentPrimary,
    topLeft = topLeft,
    size = frameSize,
    cornerRadius = corner,
    style = Stroke(width = 1.5.dp.toPx())
  )
}

/**
 * Жест полосы: касание ставит центр рамки под палец, движение ведёт её дальше.
 *
 * Порога схватывания нет намеренно — в отличие от полотна, где он отделяет протяжку от тапа по
 * узлу. Полоса это выделенный инструмент: палец, опущенный на неё, уже означает «хочу туда», и
 * ждать от него первого движения незачем.
 *
 * События потребляются: полоса перехватывает жест целиком (§11.3 брифа), и делить его с кем-то ещё
 * ей не с кем — камеру полотна она двигает сама.
 *
 * @param onScrubbingChanged идёт ли скраб: по нему показывается пузырь с датой
 * @param onScrub координата пальца внутри полосы, в пикселях
 */
private suspend fun PointerInputScope.detectScrub(
  onScrubbingChanged: (Boolean) -> Unit,
  onScrub: (Float) -> Unit
) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    down.consume()
    onScrubbingChanged(true)
    onScrub(down.position.x)
    var event: PointerEvent
    do {
      event = awaitPointerEvent()
      val change = event.changes.fastFirstOrNull { it.pressed }
      if (change != null && change.positionChanged()) {
        change.consume()
        onScrub(change.position.x)
      }
    } while (event.changes.fastAny { it.pressed })
    onScrubbingChanged(false)
  }
}

// Ключ зоны жеста: важно только то, что он один на мини-карту и не совпадает с чужим. Читается
// дважды — при объявлении зоны и при её снятии.
private val ZONE_KEY = Any()

/** Внутреннее поле полосы: читается и при рисовании, и при переводе касания в долю. */
private val TRACK_PADDING: Dp = 8.dp

/** Наименьшая рамка, которую можно взять пальцем: рисование, жест и пузырь считают её одинаково. */
private val MIN_FRAME_WIDTH: Dp = 32.dp

private val MARK_HEIGHT: Dp = 12.dp
private val FRAME_INSET: Dp = 2.dp

@Preview
@Composable
private fun GraphMinimapPreviewLight(
  @PreviewParameter(GraphMinimapPreviewProvider::class)
  minimap: GraphMinimapPreview
) {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    GraphMinimapPreviewContent(minimap)
  }
}

@Preview
@Composable
private fun GraphMinimapPreviewDark(
  @PreviewParameter(GraphMinimapPreviewProvider::class)
  minimap: GraphMinimapPreview
) {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GraphMinimapPreviewContent(minimap)
  }
}

@Composable
private fun GraphMinimapPreviewContent(minimap: GraphMinimapPreview) {
  GraphMinimap(
    modifier = Modifier.padding(vertical = 12.dp),
    span = rememberUpdatedState(minimap.span),
    marks = rememberUpdatedState(minimap.marks),
    label = rememberUpdatedState(null),
    onScrub = { },
    onBoundsChanged = { _, _ -> }
  )
}

@Preview
@Composable
private fun MinimapDateBubblePreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    MinimapDateBubble(label = rememberUpdatedState("6 мар, 09:40"))
  }
}

@Preview
@Composable
private fun MinimapDateBubblePreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    MinimapDateBubble(label = rememberUpdatedState("6 мар, 09:40"))
  }
}

@Immutable
private data class GraphMinimapPreview(
  val span: GraphViewportSpan,
  val marks: List<GraphLaneMark>
)

/**
 * Кадры превью мини-карты: длинная история, её же начало и переписка без веток.
 *
 * Пузырь с датой в кадры не входит: он показывается только пока идёт скраб, а пальца в превью нет.
 * У него своя пара превью — иначе сломанный пузырь был бы не виден до самого устройства.
 *
 * Третий кадр не украшение: переписка без веток — самый частый случай (§13 брифа), и полоса, у
 * которой рамка занимает всю ширину, а засечек нет вовсе, обязана выглядеть осмысленно.
 */
@Immutable
private class GraphMinimapPreviewProvider : PreviewParameterProvider<GraphMinimapPreview> {
  override val values = sequenceOf(
    GraphMinimapPreview(
      span = GraphViewportSpan(position = 0.45f, width = 0.06f),
      marks = listOf(
        GraphLaneMark(position = 0.08f, lane = -1),
        GraphLaneMark(position = 0.21f, lane = 1),
        GraphLaneMark(position = 0.33f, lane = -2),
        GraphLaneMark(position = 0.55f, lane = 2),
        GraphLaneMark(position = 0.71f, lane = -3),
        GraphLaneMark(position = 0.88f, lane = 3)
      )
    ),
    GraphMinimapPreview(
      span = GraphViewportSpan(position = 0f, width = 0.16f),
      marks = listOf(
        GraphLaneMark(position = 0.08f, lane = -1),
        GraphLaneMark(position = 0.21f, lane = 1)
      )
    ),
    GraphMinimapPreview(
      span = GraphViewportSpan.Full,
      marks = emptyList()
    )
  )
}
