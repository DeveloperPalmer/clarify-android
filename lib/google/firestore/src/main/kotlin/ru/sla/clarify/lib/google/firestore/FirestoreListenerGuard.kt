package ru.sla.clarify.lib.google.firestore

import android.os.Handler
import android.os.Looper
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.logError
import ru.sla.clarify.lib.google.firestore.FirestoreListenerGuard.Companion.crashOnViolation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreListenerRunawayException
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime-детектор «runaway» подписок на Firestore snapshot listener.
 *
 * Каждый `addSnapshotListener` стоит как минимум один read на первый snapshot. Если клиентский
 * код случайно пересоздаёт один и тот же listener много раз в секунду (классическая причина —
 * `flatMapLatest` поверх наблюдаемого запроса к базе, который перевыпускает одно и то же значение
 * без `distinctUntilChanged`), это незаметно сожжёт дневную квоту за минуты.
 *
 * Поведение при срабатывании: лог + (если [crashOnViolation] = true, по умолчанию) краш
 * приложения через `Handler(Looper.getMainLooper()).post { throw ... }`. Краш бросается
 * на главном потоке, чтобы дефолтный `Thread.UncaughtExceptionHandler` гарантированно
 * убил процесс — простой `throw` внутри [trackOpen] может проглотиться `callbackFlow` или
 * Firebase SDK. В проде [crashOnViolation] можно выключить, оставив только логирование.
 *
 * Использовать [trackOpen] внутри каждого `callbackFlow`, оборачивающего `addSnapshotListener`.
 *
 * Дефолты: больше 10 открытий за 1 секунду на один [trackOpen]-ключ.
 */
@SingleIn(AppScope::class)
class FirestoreListenerGuard @Inject constructor() {

  private val openHistory = ConcurrentHashMap<String, ArrayDeque<Long>>()

  fun trackOpen(key: String) {
    val now = System.currentTimeMillis()
    val deque = openHistory.getOrPut(key) { ArrayDeque() }
    val violationMessage: String? = synchronized(deque) {
      deque.addLast(now)
      while (deque.isNotEmpty() && now - deque.first() > windowMillis) {
        deque.removeFirst()
      }
      if (deque.size > openRateThreshold) {
        val size = deque.size
        // Сбрасываем, чтобы не спамить логом/крашем на каждом следующем открытии в том же окне.
        deque.clear()
        buildViolationMessage(key, size)
      } else {
        null
      }
    }

    if (violationMessage != null) {
      reportViolation(violationMessage)
    }
  }

  @Suppress("ThrowingExceptionsWithoutMessageOrCause")
  private fun buildViolationMessage(key: String, opens: Int): String {
    return buildString {
      append("Listener '").append(key).append("' opened ").append(opens)
      append(" times in ").append(windowMillis).append(" ms — runaway re-subscription. ")
      append("Almost certainly a missing distinctUntilChanged() upstream of flatMapLatest, ")
      append("or a reactive write feeding back into its own observed query. ")
      append("Stack: ").append(Throwable().stackTraceToString().take(STACK_PREVIEW_LIMIT))
    }
  }

  private fun reportViolation(message: String) {
    logError(tag = TAG) { message }
    if (!crashOnViolation) return
    // Гарантированный краш: бросаем uncaught exception на главном потоке.
    // Простой throw здесь может проглотиться callbackFlow / Firebase listener'ом.
    Handler(Looper.getMainLooper()).post {
      throw FirestoreListenerRunawayException(message)
    }
  }

  companion object {
    private const val TAG = "FirestoreGuard"
    private const val STACK_PREVIEW_LIMIT = 1500

    // Тюнеры — `var`, чтобы dev-сборки могли понижать пороги в «параноидальном» режиме,
    // а прод-сборка могла выключить краш, оставив только лог.
    var openRateThreshold: Int = 10
    var windowMillis: Long = 1_000L
    var crashOnViolation: Boolean = true
  }
}
