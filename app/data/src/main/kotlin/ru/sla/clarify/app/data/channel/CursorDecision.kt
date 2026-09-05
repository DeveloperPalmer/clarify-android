package ru.sla.clarify.app.data.channel

/**
 * Что делать с курсором после кадра.
 */
sealed interface CursorDecision {

  /** События приняты, курсор двигается на [toSeq]. */
  data class Advance(val toSeq: Long) : CursorDecision

  /**
   * Догон невозможен: нужна полная выборка. [fromSeq] — место, с которого лента идёт сейчас;
   * на него курсор и встаёт, когда выборка сделана.
   *
   * Сюда приходит и устаревший курсор, и разрыв в нумерации: снаружи это одно и то же —
   * события потеряны, и восстановить их можно только выборкой целиком.
   */
  data class Resync(val fromSeq: Long) : CursorDecision
}
