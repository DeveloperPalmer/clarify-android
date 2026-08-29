package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.Easing
import androidx.compose.ui.util.fastAny
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame

/** Полная длина церемонии слияния, мс: семь кадров §12 на одной шкале. */
internal const val MERGE_CEREMONY_MILLIS = 1200f

/** Кадр 1 — разгон пунктира и уход цвета в золото. */
private const val DASH_RUN_UP_END = 150f

/** Кадр 2 — такт без объекта: кружков одобривших в проекте нет, см. документ решения. */
private const val HELD_BEAT_END = 400f

/** Кадр 3 — на магистрали проявляется пустое кольцо: «место готово, ждёт». */
private const val RING_ARRIVAL_END = 550f

/** Кадр 4 — линия возврата идёт к кольцу. За его концом бьёт удар, и там же просыпается гаптика. */
internal const val LINE_PULL_END = 850f

/** Кадр 5 — удар: кольцо заливается, иконка растёт. */
private const val IMPACT_END = 950f

/** Кадр 6 — волна вдоль магистрали в обе стороны. */
private const val WAVE_END = 1100f

/** Период пульсации кольца: три полных цикла за кадры 3 и 4 вместе. */
private const val RING_PULSE_MILLIS = 150f

private const val DASH_SPEED_AT_REST = 1f
private const val DASH_SPEED_AT_RUN = 3f

/**
 * Кадр церемонии слияния по времени от её начала (§12 брифа).
 *
 * **Шкала одна, линейная, и она снаружи.** Каждый кадр берёт от неё свой отрезок и применяет свою
 * кривую внутри — так семь кадров остаются частями одного движения. Обратный порядок, где у каждого
 * кадра своя анимация со своим стартом, дал бы им разъехаться на первом же пропущенном кадре
 * отрисовки, и заметить это можно было бы только глазом на устройстве.
 *
 * Золото не гаснет само: [MergeCeremonyFrame.gold] поднимается кадром 1, держится всю церемонию и
 * **возвращается к цвету идентичности кадром 7**, вместе с выдохом. Иначе линия осталась бы золотой
 * навсегда, а золото по §7 — не идентичность ветки, а событие.
 *
 * Время зажимается в границы шкалы, а не считается по формуле за ними: отрицательное значение дало
 * бы отрицательную долю, а превышающее — долю больше единицы, и то и другое ушло бы в рисование
 * молча.
 *
 * @param elapsedMillis время от начала церемонии; за границами шкалы зажимается
 * @param decelerate кривая затухания без разгона — `AppMotion.decelerate`. Приходит параметром, а не
 *   читается из темы, чтобы функция осталась чистой и проверяемой без устройства
 * @return доли всех семи кадров на этот момент
 */
internal fun mergeCeremonyFrameOf(elapsedMillis: Float, decelerate: Easing): MergeCeremonyFrame {
  val elapsed = elapsedMillis.coerceIn(0f, MERGE_CEREMONY_MILLIS)
  val runUp = spanFractionOf(elapsed, 0f, DASH_RUN_UP_END)
  val exhale = spanFractionOf(elapsed, WAVE_END, MERGE_CEREMONY_MILLIS)
  return MergeCeremonyFrame(
    dashSpeed = DASH_SPEED_AT_REST + (DASH_SPEED_AT_RUN - DASH_SPEED_AT_REST) * runUp,
    gold = runUp * (1f - exhale),
    ring = spanFractionOf(elapsed, HELD_BEAT_END, RING_ARRIVAL_END),
    ringPulse = ringPulseOf(elapsed),
    reach = decelerate.transform(spanFractionOf(elapsed, RING_ARRIVAL_END, LINE_PULL_END)),
    impact = spanFractionOf(elapsed, LINE_PULL_END, IMPACT_END),
    wave = spanFractionOf(elapsed, IMPACT_END, WAVE_END),
    exhale = exhale
  )
}

/**
 * Фаза пульсации кольца: цикл длиной [RING_PULSE_MILLIS], пока кольцо ждёт линию.
 *
 * Ждёт оно кадры 3 и 4 — §6.6 брифа отводит кольцу ровно столько, — а до и после пульсации нет:
 * до кольца ещё нет вовсе, после его заливает удар.
 */
private fun ringPulseOf(elapsed: Float): Float {
  if (elapsed < HELD_BEAT_END || elapsed >= LINE_PULL_END) {
    return 0f
  }
  return (elapsed - HELD_BEAT_END) % RING_PULSE_MILLIS / RING_PULSE_MILLIS
}

/** Доля отрезка шкалы, пройденная к моменту [elapsed]: `0` до начала отрезка, `1` после конца. */
private fun spanFractionOf(elapsed: Float, from: Float, to: Float): Float {
  return ((elapsed - from) / (to - from)).coerceIn(0f, 1f)
}

/**
 * Есть ли на графе чему бежать: только тогда заводится фаза пунктира.
 *
 * Предикат спрашивает про **два** источника, а не про один, и второй здесь не украшение. Бегущий
 * пунктир §7 принадлежит ветке, готовой к слиянию, — но церемония переигрывается по чипу ветки уже
 * **слитой**, у которой штрих сплошной, а альфа 60 %. Пока предикат смотрел только на статус, кадру 1
 * нечего было ускорять, а кадру 7 — не из чего гаснуть: оба играли бы в пустоту, не уронив ни одного
 * теста и не показав ни одной ошибки.
 *
 * Бесконечная анимация запрашивает кадр, пока жива, поэтому на графе, где бежать нечему, её быть не
 * должно вовсе: иначе это вечный кадр ни для чего.
 *
 * @param edges рёбра этого кадра
 * @param ceremonyPlaying играет ли церемония слияния
 * @return нужна ли фаза бегущего пунктира
 */
internal fun isDashRunning(edges: List<GraphEdge>, ceremonyPlaying: Boolean): Boolean {
  return ceremonyPlaying || edges.fastAny { it.status == GraphBranchStatus.Ready }
}

/**
 * Играет ли церемония судьбу этого ребра.
 *
 * Спрашивается идентификатор, а не цвет: оттенков шесть, и по цвету в ответ попала бы каждая шестая
 * ветка переписки заодно — см. KDoc [GraphEdge].
 *
 * @param edge проверяемое ребро
 * @param ceremonyBranch ветка играющей церемонии; `null` — церемония не играет
 * @return принадлежит ли ребро сливающейся ветке
 */
internal fun isCeremonyEdge(edge: GraphEdge, ceremonyBranch: GraphBranch.Id?): Boolean {
  return ceremonyBranch != null && edge.branchId == ceremonyBranch
}

/**
 * Прозрачность линии сливающейся ветки на кадре церемонии.
 *
 * Всю церемонию линия идёт в полную силу — иначе кадру 7 не из чего было бы гаснуть, — и ровно
 * выдохом приходит к тем же 60 %, которыми `toEdgeAlpha` рисует ветку в покое. Числа обязаны
 * сойтись: разойдись они, конец церемонии дёрнул бы линию скачком.
 *
 * @param frame кадр церемонии
 * @return прозрачность линии, от `1` до `0.6`
 */
internal fun ceremonyEdgeAlphaOf(frame: MergeCeremonyFrame): Float {
  return 1f - (1f - MERGED_EDGE_ALPHA) * frame.exhale
}

/** Прозрачность линии слитой ветки в покое: §7 гасит её до 60 %. */
internal const val MERGED_EDGE_ALPHA = 0.6f
