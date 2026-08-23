package ru.sla.clarify.feature.chronology.ui.components.canvas

/**
 * Наклон семейства диагоналей ромбовидного фона.
 *
 * Два семейства различаются знаком в уравнении прямой, и от него же зависит, с какого края линии
 * въезжают во вьюпорт при панорамировании.
 */
internal enum class DiagonalSlope {

  /** Вниз-вправо: прямая задаётся `x - y = c`. */
  DownRight,

  /** Вниз-влево: прямая задаётся `x + y = c`. */
  DownLeft
}
