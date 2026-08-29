package ru.sla.clarify.feature.chronology.ui.components.canvas

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Просит ли пользователь обойтись без движения (§14 брифа, вопрос §18 п. 11).
 *
 * **Флаг живёт здесь, а не в `AppMotion`** — решение владельца. Все его потребители сегодня в
 * хронологии, а глобальный вариант сделал бы держатель токенов зависимым от состояния: `AppMotion`
 * заводится как `remember { AppMotion() }` без ключей, и токен длительности, меняющийся под флагом,
 * начал бы врать о себе всему приложению разом. Обратный ход дешёвый: понадобится второму экрану —
 * файл переезжает в `uikit` без правки потребителей.
 *
 * Источник — `ANIMATOR_DURATION_SCALE`, тот самый ноль, которым система выключает анимации во всём
 * приложении. Собственной настройки заводить некуда: персистентного key-value в проекте нет (§18
 * п. 12), а спрашивать пользователя о том, что он уже сказал системе, — значит спросить дважды.
 *
 * Значение **наблюдается**, а не снимается один раз. Снятое на входе, оно пережило бы включение
 * флага в настройках и вернуло бы движение тому, кто от него отказался, — а активность на смену
 * этого параметра не пересоздаётся, и заметить подмену было бы нечем.
 *
 * @return `true`, если движение просят выключить
 */
@Composable
internal fun rememberReducedMotion(): Boolean {
  val resolver = LocalContext.current.contentResolver
  var reduced by remember(resolver) { mutableStateOf(resolver.isMotionOff()) }
  DisposableEffect(resolver) {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        reduced = resolver.isMotionOff()
      }
    }
    resolver.registerContentObserver(
      Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
      false,
      observer
    )
    onDispose { resolver.unregisterContentObserver(observer) }
  }
  return reduced
}

/**
 * Выключены ли анимации системно.
 *
 * Сравнение с нулём, а не «меньше единицы»: множитель 0.5 означает «быстрее», а не «не надо
 * двигаться», и церемония, вырезанная у того, кто всего лишь ускорил переходы, была бы потерей без
 * причины.
 */
private fun ContentResolver.isMotionOff(): Boolean {
  return Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
