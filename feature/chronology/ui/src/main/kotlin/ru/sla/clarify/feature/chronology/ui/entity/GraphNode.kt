package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Узел графа с точки зрения раскладки: где он стоит, но не что в нём нарисовано.
 *
 * Содержимое узла даёт вызывающий по [id] — полотну оно безразлично. Здесь только то, из чего
 * считается положение, чтобы раскладку можно было проверить юнит-тестом без Compose.
 *
 * @param id идентификатор узла, он же ключ композиции
 * @param lane дорожка: `0` — магистраль, отрицательные вверх, положительные вниз
 * @param gap пауза перед этим узлом относительно предыдущего узла списка
 */
@Immutable
data class GraphNode(
  val id: Id,
  val lane: Int,
  val gap: TimeGap
) {

  /** Идентификатор узла графа: ключ композиции и будущая цель перехода в чат. */
  @JvmInline
  value class Id(val value: String)
}
