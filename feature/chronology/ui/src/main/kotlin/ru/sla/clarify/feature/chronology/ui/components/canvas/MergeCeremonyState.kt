package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.sla.atlas.entity.Branch
import ru.sla.clarify.feature.chronology.ui.entity.MergeCeremonyFrame
import ru.sla.clarify.uikit.theme.AppTheme

/**
 * Держатель церемонии слияния: какая ветка её играет и на какой отметке шкалы находится.
 *
 * @return держатель, живущий до выхода с экрана
 */
@Composable
internal fun rememberMergeCeremonyState(): MergeCeremonyState {
  // Кривая приходит ключом, а не читается держателем: `AppMotion` живёт в теме, а держатель —
  // обычный класс, и чтение темы изнутри сделало бы его непроверяемым без Compose.
  val decelerate = AppTheme.motion.decelerate
  return remember(decelerate) { MergeCeremonyState(decelerate) }
}

/**
 * Церемония слияния как состояние: одна шкала, одна ветка, один ход (§12 брифа).
 *
 * **Церемония не оверлей, а подмена в рисуемом.** Слитая ветка рисуется сплошной линией в 60 %
 * силы, и кадру 1 нечего было бы ускорять, а кадру 7 — не из чего гаснуть. Поэтому на время
 * церемонии ветка показывается так, будто она снова готова к слиянию, и лишь выдох возвращает её к
 * покою. Отсюда же следует, что церемония обязана попасть в тот же слой, что и рёбра, а не лечь
 * поверх него: слой этот — камера, то есть `graphicsLayer`, и всё, что рисуется мимо него,
 * разъезжается с графом на первом же панорамировании.
 *
 * Наружу отдаются [State], а не значения: значение заставило бы читателя подписаться там, где он его
 * получил, а `State` читается ровно в той фазе, которой нужен, — здесь это фаза рисования.
 *
 * @param decelerate кривая затухания без разгона для кадра 4; `AppMotion.decelerate`
 */
@Stable
internal class MergeCeremonyState(private val decelerate: Easing) {

  /** Отметка на шкале церемонии, мс. Снапшотное: читается в рисовании, и запись обязана его позвать. */
  private var elapsed by mutableFloatStateOf(0f)

  /** Чья церемония идёт; `null` — не идёт ничья. */
  private var playedBranch by mutableStateOf<Branch.Id?>(null)

  /**
   * Ход церемонии.
   *
   * Поле обычное, а не снапшотное, и это то же решение, что у `motionJob` полотна: за ходом никто
   * не подписывается, а снапшотное состояние здесь означало бы запись из фазы композиции.
   */
  private var job: Job? = null

  /** Ветка, чья церемония играет прямо сейчас. */
  val branch: State<Branch.Id?> = derivedStateOf { playedBranch }

  /** Играет ли церемония вообще: из этого же выводится, есть ли на графе чему бежать. */
  val playing: State<Boolean> = derivedStateOf { playedBranch != null }

  /** Кадр на текущей отметке шкалы; `null`, когда церемония не играет. */
  val frame: State<MergeCeremonyFrame?> = derivedStateOf {
    playedBranch?.let { mergeCeremonyFrameOf(elapsed, decelerate) }
  }

  /**
   * Проигрывает церемонию для ветки [branchId] с начала.
   *
   * **Повторный вызов перезапускает, а не ставит в очередь.** Фаза пунктира одна на весь граф, и две
   * церемонии сразу служили бы двум господам: разгон второй читался бы поверх разгона первой.
   *
   * Под [reduced] шкала не гонится вовсе — ни одного движущегося пикселя. Это и есть то, чего просит
   * §14: граф остаётся в покое слитой ветки, а подтверждением служит гаптика, которую зовёт
   * вызывающий. Кроссфейд §14 виден только на живом пути, где точка слияния появляется впервые; на
   * переигрывании ей неоткуда и некуда меняться — она уже залита.
   *
   * @param scope область, переживающая композицию узла, по которому нажали
   * @param branchId ветка, чью церемонию играем
   * @param reduced просит ли пользователь обойтись без движения
   * @param onImpact удар кадра 5: зовётся ровно на границе 850 мс и ровно один раз за проигрывание.
   *   Под [reduced] зовётся сразу — отклик это не движение, и глушить его вместе с анимацией значило
   *   бы отнять у незрячего единственный сигнал, что слияние состоялось
   */
  fun play(
    scope: CoroutineScope,
    branchId: Branch.Id,
    reduced: Boolean,
    onImpact: () -> Unit
  ) {
    stop()
    if (reduced) {
      onImpact()
      return
    }
    playedBranch = branchId
    elapsed = 0f
    job = scope.launch { run(onImpact) }
  }

  /** Обрывает церемонию и возвращает граф к покою. */
  fun stop() {
    job?.cancel()
    job = null
    playedBranch = null
    elapsed = 0f
  }

  private suspend fun run(onImpact: () -> Unit) {
    var struck = false
    // Кривая шкалы линейна намеренно: свою кривую применяет каждый кадр внутри себя, и вторая,
    // наложенная на всю шкалу, растянула бы одни кадры за счёт других.
    Animatable(0f).animateTo(
      targetValue = MERGE_CEREMONY_MILLIS,
      animationSpec = tween(
        durationMillis = MERGE_CEREMONY_MILLIS.toInt(),
        easing = LinearEasing
      )
    ) {
      elapsed = value
      // Ровно один раз за проигрывание: кадры анимации приходят чаще, чем раз в 850 мс, и без
      // засова удар дребезжал бы каждым кадром до конца шкалы.
      if (!struck && value >= LINE_PULL_END) {
        struck = true
        onImpact()
      }
    }
    playedBranch = null
    elapsed = 0f
  }
}
