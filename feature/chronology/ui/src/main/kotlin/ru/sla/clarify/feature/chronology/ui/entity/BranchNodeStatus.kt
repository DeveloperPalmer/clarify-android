package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Что происходит с веткой — строка статуса на её узле (§6.5 брифа).
 *
 * Пять состояний собираются из трёх доменных: `MergeRequest` у ветки может не быть вовсе
 * ([Active]), может быть открыт ([Waiting]), готов ([Ready]) или завершён ([Merged]). Пятое,
 * [Abandoned], в домене не хранится ни в каком виде и считается по времени последнего сообщения —
 * поэтому статус узла и есть отдельная сущность экрана, а не доменный `enum`, вынесенный в `ui`.
 *
 * Дни молчания лежат внутри [Abandoned], а не отдельным параметром рядом: два значения, которые
 * всегда двигаются вместе, — способ рано или поздно передать одно без другого.
 */
@Immutable
sealed interface BranchNodeStatus {

  /** Тема живёт: merge request не открыт. */
  data object Active : BranchNodeStatus

  /** Merge request открыт и ждёт одобрений. */
  data object Waiting : BranchNodeStatus

  /** Одобрен обоими — осталось завершить слияние. */
  data object Ready : BranchNodeStatus

  /** Тема закрыта, ветка стала read-only. Непрочитанное в ней при этом остаётся. */
  data object Merged : BranchNodeStatus

  /**
   * Тему бросили: сообщений нет дольше тридцати дней, merge request не открывали.
   *
   * @param silentDays сколько дней ветка молчит
   */
  data class Abandoned(val silentDays: Int) : BranchNodeStatus
}
