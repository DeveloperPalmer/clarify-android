package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Edge
import ru.sla.atlas.entity.EdgeRole
import ru.sla.atlas.ui.addOrthogonalRoute
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame
import ru.sla.clarify.uikit.theme.AppColors
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Чем полотно хронологии красит свои связи.
 *
 * Маршрут линии считает раскладка, а собирает `Path.addOrthogonalRoute`; здесь решается всё
 * остальное — толщина по роли, штрих по статусу ветки, золото и обрезка по кадру церемонии,
 * растворение хвоста. Ни одно из этих правил не свойство полотна: они читаются из §7 и §12 брифа,
 * и полотну, которое их исполняет, знать о них незачем.
 *
 * @param edges рёбра графа этого кадра
 * @param ceremony церемония слияния: полотно её не запускает, но линия готовой к слиянию ветки
 *   бежит пунктиром, а на время церемонии так же показывается и слитая
 * @return то, чем полотно рисует свои связи внутри слоя камеры
 */
@Composable
internal fun rememberEdgePainter(edges: State<List<Edge>>, ceremony: MergeCeremonyState): DrawScope.() -> Unit {
  val colors = AppTheme.colors
  // Путь один на все рёбра и чистится `rewind()`, а не создаётся заново: он держит выделенную
  // память между вызовами, и сотня рёбер иначе рождала бы сотню нативных объектов на каждый проход.
  val edgePath = remember { Path() }
  // Второй путь и мерка — только для кадра 4: маршрут возврата рисуется не целиком, а до отметки
  // `reach`. Оба переиспользуются между кадрами по той же причине, что и сам `edgePath`.
  val reachedPath = remember { Path() }
  val pathMeasure = remember { PathMeasure() }
  val density = LocalDensity.current
  // Ключ — плотность: спека, замороженная от старого экрана, уже однажды стоила фиче дефекта.
  val dashIntervals = remember(density) {
    with(density) { floatArrayOf(6.dp.toPx(), 4.dp.toPx()) }
  }
  // Неподвижный пунктир кэшируется, потому что `AndroidPathEffect` не имеет `equals`: `Stroke`
  // сравнивает его по ссылке, и новый экземпляр на каждом кадре означал бы новый нативный
  // `DashPathEffect` и вызов `Paint.setPathEffect` на каждое ребро.
  val dashEffect = remember(dashIntervals) { PathEffect.dashPathEffect(dashIntervals) }
  // Бежит пунктир только у веток, готовых к слиянию (§7), а таких на графе может не быть вовсе.
  // Фаза заводится ровно тогда, когда есть чему бежать: бесконечная анимация запрашивает кадр,
  // пока жива, а её чтение в рисовании перерисовывает слой связей каждый кадр — на графе без
  // готовых веток это был бы вечный кадр ни для чего.
  val hasRunningEdge = remember(edges, ceremony) {
    derivedStateOf { isDashRunning(edges.value, ceremony.playing.value) }
  }
  val dashPhase = rememberDashPhase(period = dashIntervals.sum(), isRunning = hasRunningEdge.value)
  val cornerRadius = with(density) { 8.dp.toPx() }
  val hopRadius = with(density) { 6.dp.toPx() }
  val fadeLength = with(density) { 40.dp.toPx() }
  // Кадр 6 расходится на 120 dp в каждую сторону — три ширины точки слияния. §12 говорит «короткая
  // вспышка», числа не даёт; это число выбрано здесь и ждёт взгляда на устройстве.
  val waveSpread = with(density) { 120.dp.toPx() }
  return {
    // Фаза одна на все рёбра, поэтому и нативный объект на кадр создаётся один, а не по одному
    // на ребро. Кэшировать его, как неподвижный, нельзя: в фазе и состоит весь бег.
    val frame = ceremony.frame.value
    val ceremonyBranch = ceremony.branch.value
    val frameEdges = edges.value
    val runningDashEffect = dashPhase?.let { phase ->
      // Разгон кадра 1 — это множитель на фазу: узор периодичен, и фаза, идущая втрое дальше за
      // тот же цикл, и есть тот же бег втрое быстрее. Отдельной анимации разгону не нужно.
      PathEffect.dashPathEffect(dashIntervals, phase.value * (frame?.dashSpeed ?: 1f))
    }
    // Точку слияния берём у самого маршрута возврата: его последняя точка и лежит на магистрали.
    // Спрашивать её у раскладки значило бы завести второй источник того же числа.
    val mergePoint = frame?.let {
      frameEdges.firstOrNull { edge ->
        edge.role == EdgeRole.Merge && isCeremonyEdge(edge, ceremonyBranch)
      }?.points?.last()
    }
    frameEdges.fastForEach { edge ->
      drawGraphEdge(
        edge = edge,
        path = edgePath,
        colors = colors,
        dashEffect = dashEffect,
        runningDashEffect = runningDashEffect,
        cornerRadius = cornerRadius,
        hopRadius = hopRadius,
        fadeLength = fadeLength,
        frame = frame.takeIf { isCeremonyEdge(edge, ceremonyBranch) },
        reachedPath = reachedPath,
        pathMeasure = pathMeasure
      )
    }
    if (frame != null && mergePoint != null) {
      drawMergeWave(
        edges = frameEdges,
        path = edgePath,
        colors = colors,
        frame = frame,
        mergePoint = mergePoint,
        cornerRadius = cornerRadius,
        hopRadius = hopRadius,
        spread = waveSpread
      )
    }
  }
}

/**
 * Фаза бегущего пунктира — сдвиг узора вдоль линии, в пикселях полотна.
 *
 * Фаза идёт **вниз**, от нуля к минус периоду, и это не описка: фаза сдвигает узор назад по пути,
 * поэтому вперёд — от развилки к слиянию — пунктир бежит при убывающей. На стыке итераций значение
 * прыгает с минус периода в ноль, но узор периодичен, и прыжок этот невидим по построению.
 *
 * Бег живёт в координатах полотна, а не экрана: слой масштабируется целиком, поэтому на 2.5× вместе
 * со штрихом растягивается и скорость. Это то же правило, по которому там же толстеет сама линия.
 *
 * @param period длина одного повтора узора: штрих плюс пробел, в пикселях
 * @param isRunning есть ли на графе ветка, готовая к слиянию
 * @return фаза или `null`, если бежать нечему
 */
@Composable
private fun rememberDashPhase(period: Float, isRunning: Boolean): State<Float>? {
  if (!isRunning) {
    return null
  }
  val transition = rememberInfiniteTransition(label = "dash")
  return transition.animateFloat(
    label = "phase",
    initialValue = 0f,
    targetValue = -period,
    animationSpec = AppTheme.motion.loopTween()
  )
}

/**
 * Рисует одно ребро: собирает путь по точкам излома и кладёт на него штрих.
 *
 * `Path` приходит снаружи и чистится `rewind()`: он переиспользуется между рёбрами, иначе каждый
 * проход слоя рождал бы по нативному объекту на ребро.
 *
 * Маршрут собирает [addOrthogonalRoute]: где линия поворачивает и где горбится, знает геометрия, а
 * здесь решается только, чем её провести.
 *
 * @param edge ребро в координатах полотна
 * @param path переиспользуемый путь
 * @param colors палитра активной темы
 * @param dashEffect кэшированный неподвижный пунктир
 * @param runningDashEffect тот же пунктир, сдвинутый на фазу этого кадра; `null`, если готовых к
 *   слиянию веток на графе нет
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика над чужой вертикалью
 * @param fadeLength длина растворения хвоста
 * @param frame кадр церемонии, если её играет **это** ребро; `null` — ребро рисуется своим статусом
 * @param reachedPath путь под обрезанный кадром 4 маршрут возврата
 * @param pathMeasure мерка для той же обрезки
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
private fun DrawScope.drawGraphEdge(
  edge: Edge,
  path: Path,
  colors: AppColors,
  dashEffect: PathEffect,
  runningDashEffect: PathEffect?,
  cornerRadius: Float,
  hopRadius: Float,
  fadeLength: Float,
  frame: MergeCeremonyFrame? = null,
  reachedPath: Path? = null,
  pathMeasure: PathMeasure? = null
) {
  // Кадр 4 рисует возврат по частям, и до его начала рисовать нечего вовсе.
  if (frame != null && edge.role == EdgeRole.Merge && frame.reach <= 0f) {
    return
  }
  path.rewind()
  path.addOrthogonalRoute(edge, cornerRadius, hopRadius)
  val identity = edge.color
  // Кадр 1 уводит цвет в золото, кадр 7 возвращает его ветке: золото по §7 — событие, а не
  // идентичность, и линия, оставшаяся золотой, соврала бы о том, чья она.
  val color = frame?.let { lerp(identity, colors.contentGoldPrimary, it.gold) } ?: identity
  val width = when (edge.role) {
    EdgeRole.Baseline -> 2.dp.toPx()
    // Слой ответов §7 рисуется 1 dp, но его здесь нет: он живёт только внутри раскрытого эпизода.
    EdgeRole.Branch, EdgeRole.Fork, EdgeRole.Merge, EdgeRole.Tail -> 1.5.dp.toPx()
  }
  // На время церемонии ветка показывается готовой к слиянию: у слитой штрих сплошной, и кадру 1
  // нечего было бы ускорять. Покой возвращает выдох, а не конец шкалы.
  val status = if (frame != null) Branch.Status.Ready else edge.status
  val alpha = frame?.let { ceremonyEdgeAlphaOf(it) } ?: edge.status.toEdgeAlpha()
  val style = Stroke(
    width = width,
    // Кап тупой, а не круглый, хотя прототип берёт круглый: тот добавляет по половине толщины с
    // каждой стороны штриха, и пробел 4 dp читается как 2.5 dp, а на 0.4× пунктир сливается в
    // сплошную линию. Штрих — единственное, что отличает «живёт» от «MR открыт» помимо иконки.
    cap = StrokeCap.Butt,
    join = StrokeJoin.Round,
    pathEffect = status.toPathEffect(dashEffect, runningDashEffect)
  )
  if (frame != null && edge.role == EdgeRole.Merge && reachedPath != null && pathMeasure != null) {
    // Кадр 4: конец линии идёт по маршруту к кольцу. Отметка 0 у мерки лежит у последнего узла
    // ветки, длина — на магистрали, поэтому отрезок `[0, длина · reach]` и есть пройденный путь.
    pathMeasure.setPath(path, false)
    reachedPath.rewind()
    pathMeasure.getSegment(0f, pathMeasure.length * frame.reach, reachedPath, true)
    drawPath(path = reachedPath, color = color, alpha = alpha, style = style)
    return
  }
  if (edge.role == EdgeRole.Tail) {
    // Хвост растворяется у своего конца: линия не обрывается стеной, а «продолжается в будущее».
    val end = edge.points.last()
    drawPath(
      path = path,
      brush = Brush.horizontalGradient(
        colorStops = arrayOf(
          0f to color.copy(alpha = alpha),
          1f to color.copy(alpha = 0f)
        ),
        startX = maxOf(edge.points.first().x, end.x - fadeLength),
        endX = end.x
      ),
      style = style
    )
  } else {
    drawPath(path = path, color = color, alpha = alpha, style = style)
  }
}

/**
 * Кадр 6: короткая вспышка вдоль магистрали в обе стороны от точки слияния.
 *
 * Рисуется вторым проходом по тем же рёбрам магистрали, а не своей геометрией: у волны нет
 * собственного маршрута — она бежит по линии, которая уже есть, и повторять её изгибы значило бы
 * завести второй источник одной и той же ломаной.
 *
 * Расхождение и угасание идут из одной доли: волна тем шире, чем слабее. Так вспышка кончается
 * растворением, а не обрывом.
 *
 * @param edges рёбра этого кадра
 * @param path переиспользуемый путь
 * @param colors палитра активной темы
 * @param frame кадр церемонии
 * @param mergePoint точка слияния на магистрали
 * @param cornerRadius радиус скругления углов
 * @param hopRadius радиус мостика
 * @param spread наибольшее расхождение волны в каждую сторону
 */
@Suppress("LongParameterList")
private fun DrawScope.drawMergeWave(
  edges: List<Edge>,
  path: Path,
  colors: AppColors,
  frame: MergeCeremonyFrame,
  mergePoint: Offset,
  cornerRadius: Float,
  hopRadius: Float,
  spread: Float
) {
  if (frame.wave <= 0f || frame.wave >= 1f) {
    return
  }
  val reach = spread * frame.wave
  val style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt, join = StrokeJoin.Round)
  val brush = Brush.horizontalGradient(
    colorStops = arrayOf(
      0f to colors.contentGoldPrimary.copy(alpha = 0f),
      // 30 % в §12 — это альфа в начале кадра, и она гаснет вместе с расхождением.
      0.5f to colors.contentGoldPrimary.copy(alpha = 0.3f * (1f - frame.wave)),
      1f to colors.contentGoldPrimary.copy(alpha = 0f)
    ),
    startX = mergePoint.x - reach,
    endX = mergePoint.x + reach
  )
  edges.fastForEach { edge ->
    if (edge.role != EdgeRole.Baseline) {
      return@fastForEach
    }
    path.rewind()
    path.addOrthogonalRoute(edge, cornerRadius, hopRadius)
    drawPath(path = path, brush = brush, style = style)
  }
}

/**
 * Прозрачность линии по состоянию ветки: §7 гасит слитую до 60 %, остальные идут в полную силу.
 *
 * Число берётся из [MERGED_EDGE_ALPHA], а не пишется здесь: к нему же кадром 7 приходит выдох
 * церемонии, и разойдись эти два места, конец церемонии дёрнул бы линию скачком.
 */
private fun Branch.Status.toEdgeAlpha(): Float {
  return when (this) {
    Branch.Status.Merged -> MERGED_EDGE_ALPHA
    Branch.Status.Alive, Branch.Status.Waiting, Branch.Status.Ready -> 1f
  }
}

/**
 * Штрих по состоянию ветки: §7 даёт замороженной пунктир, а готовой к слиянию — тот же пунктир, но
 * бегущий.
 *
 * Узор у обоих один, и это не экономия: «MR открыт» и «одобрен обоими» — соседние состояния одной
 * заморозки, и разный узор объявил бы их разными по природе. Движение же читается как «дело
 * доведено до конца и ждёт только нажатия».
 *
 * @param dash неподвижный пунктир
 * @param runningDash пунктир, сдвинутый на фазу кадра; `null` означает, что готовых веток на графе
 *   нет и фазу никто не считает
 * @return эффект штриха или `null` у сплошной линии
 */
private fun Branch.Status.toPathEffect(dash: PathEffect, runningDash: PathEffect?): PathEffect? {
  return when (this) {
    Branch.Status.Waiting -> dash
    Branch.Status.Ready -> runningDash ?: dash
    Branch.Status.Alive, Branch.Status.Merged -> null
  }
}
