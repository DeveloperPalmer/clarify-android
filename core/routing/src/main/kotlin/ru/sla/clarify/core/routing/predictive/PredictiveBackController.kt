package ru.sla.clarify.core.routing.predictive

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.kode.way.Path

/**
 * Держит всё анимируемое состояние жеста predictive back и инкапсулирует его жизненный цикл — протяжку,
 * выполнение «назад», отмену и возврат в покой. Хост ([PredictiveNodeHost]) лишь дёргает методы из обработчика
 * жеста и читает значения для отрисовки слоёв.
 *
 * Выполнение «назад» и отмена запускаются на внешнем [CoroutineScope] хоста, а не на корутине самого жеста:
 * её платформа отменяет в момент релиза, а доиграть финальную анимацию нужно уже после этого.
 */
@Stable
internal class PredictiveBackController {

  /** Scale-прогресс [0;1]: им управляет палец во время протяжки, при выполнении «назад» дожимается до 1. */
  val scaleProgress = Animatable(0f)

  /** Прогресс перехода «назад» [0;1] (cross-fade): 0 во время протяжки, 1 после релиза за порогом. */
  val backProgress = Animatable(0f)

  /** Вертикальная позиция пальца в пикселях — за ней следуют оба экрана. */
  var touchY: Float by mutableFloatStateOf(0f)
    private set

  /** Идёт ли predictive-жест: взводится на первом кадре протяжки и держится до конца финальной анимации. */
  var isGestureActive: Boolean by mutableStateOf(false)
    private set

  /** Пришёл ли хоть один кадр протяжки. false → «назад» был мгновенным (кнопка навбара / API < 34). */
  var hasDragged: Boolean by mutableStateOf(false)
    private set

  /** Идёт ли выполнение «назад» (активный узел вот-вот сменится на нижний экран). */
  var isNavigatingBack: Boolean by mutableStateOf(false)
    private set

  /** Путь экрана, с которого стартовал жест, — чтобы отличать «до» и «после» подмены активного узла. */
  var draggedPath: Path? by mutableStateOf(null)
    private set

  /** Момент старта текущего жеста (uptime, мс) — от него отмеряется задержка активации predictive-слоёв. */
  private var gestureStartMs: Long = 0L

  /**
   * Сырой `progress` платформы в момент активации жеста. Из-за задержки активации к этому моменту палец уже
   * немного протянул (progress ~0.05–0.1), поэтому прогресс ремапится в диапазон [activationProgress; 1] → [0; 1]:
   * scale стартует ровно с 0 (экран полноразмерный, без скачка) и плавно растёт по мере дальнейшей протяжки.
   */
  private var activationProgress: Float = 0f

  /**
   * Готовит контроллер к новому жесту:
   * Запоминает экран старта, момент старта и сбрасывает прогресс.
   * [isGestureActive] здесь НЕ взводится — predictive-слои включаются лишь когда жест окажется осознанной
   * протяжкой (см. [applyDrag]), иначе мгновенный «назад» кнопкой или быстрый флик ужимали бы экраны зря.
   */
  suspend fun beginGesture(path: Path?) {
    isNavigatingBack = false
    hasDragged = false
    draggedPath = path
    gestureStartMs = SystemClock.uptimeMillis()
    scaleProgress.snapTo(0f)
    backProgress.snapTo(0f)
  }

  /**
   * Применяет текущий кадр протяжки.
   * Жест активируется (включает predictive-слои) лишь когда он оказываетсяосознанной протяжкой:
   * - продлился дольше [GESTURE_ACTIVATION_DELAY_MS]
   * - набрал прогресс выше [DRAG_ACTIVATION_THRESHOLD].
   *
   * Так отсекаются оба не-predictive случая:
   * - системная кнопка «назад» (3-кнопочная навигация) шлёт один кадр `onBackStarted` с `progress == 0`;
   * - быстрый флик-свайп края длится ~30–40 мс и завершается мгновенным back.
   *
   * Реальная протяжка длится ~200 мс и набирает заметный прогресс, поэтому активирует жест и анимируется.
   */
  suspend fun applyDrag(progress: Float, touchY: Float) {
    val dragProgress = progress.coerceIn(0f, 1f)
    if (!hasDragged) {
      val elapsedMs = SystemClock.uptimeMillis() - gestureStartMs
      if (elapsedMs < GESTURE_ACTIVATION_DELAY_MS || dragProgress <= DRAG_ACTIVATION_THRESHOLD) {
        return
      }
      hasDragged = true
      isGestureActive = true
      activationProgress = dragProgress
    }
    this.touchY = touchY
    val span = 1f - activationProgress
    val remapped = if (span > 0f) {
      ((dragProgress - activationProgress) / span).coerceIn(0f, 1f)
    } else {
      1f
    }
    scaleProgress.snapTo(DECELERATE.transform(remapped))
  }

  /**
   * Подтверждает жест и выполняет «назад»:
   * Параллельно дожимает scale и проигрывает cross-fade (верхний фейдит,scrim снимается),
   * затем вызывает [onBack], чтобы смена активного узла произошла уже за завершённой анимацией.
   */
  fun performPredictiveBack(scope: CoroutineScope, onBack: () -> Unit) {
    scope.launch {
      val animationSpec = tween<Float>(
        easing = DECELERATE,
        durationMillis = BACK_ANIM_MS
      )
      launch { scaleProgress.animateTo(1f, animationSpec) }
      backProgress.animateTo(1f, animationSpec)
      isNavigatingBack = true
      onBack()
    }
  }

  /**
   * Мгновенный «назад» без predictive-анимации:
   * Протяжки не было ([hasDragged] == false), predictive-слои не показывались и [isGestureActive]
   * остался false, поэтому экраны не трансформированы. Просто отдаём навигацию — обычный переход
   * доиграет `AnimatedContent` хоста, как в штатном `NodeHost` (размеры экранов не меняются).
   */
  fun performInstantBack(onBack: () -> Unit) {
    onBack()
  }

  /** Отменяет жест: возвращает scale в покой; навигация при этом не происходит. */
  fun cancel(scope: CoroutineScope) {
    scope.launch {
      scaleProgress.animateTo(targetValue = 0f, animationSpec = tween(CANCEL_ANIM_MS))
      isGestureActive = false
    }
  }

  /** Сбрасывает контроллер в покой после того, как «назад» сменил активный узел на нижний экран. */
  suspend fun settle() {
    isNavigatingBack = false
    isGestureActive = false
    scaleProgress.snapTo(0f)
    backProgress.snapTo(0f)
  }
}

// Easing STANDARD_DECELERATE, рекомендованный для прогресса predictive back (PathInterpolator(0, 0, 0, 1)).
private val DECELERATE = CubicBezierEasing(0f, 0f, 0f, 1f)

// Минимальный прогресс протяжки, с которого считаем жест реальным predictive-свайпом. Нулевой кадр
// onBackStarted от системной кнопки «назад» (3-кнопочная навигация) остаётся ниже порога и не запускает анимацию.
private const val DRAG_ACTIVATION_THRESHOLD = 0.01f

// Сколько жест должен длиться, прежде чем считать его осознанной протяжкой и включить predictive-слои.
private const val GESTURE_ACTIVATION_DELAY_MS = 100L

private const val BACK_ANIM_MS = 250
private const val CANCEL_ANIM_MS = 220
