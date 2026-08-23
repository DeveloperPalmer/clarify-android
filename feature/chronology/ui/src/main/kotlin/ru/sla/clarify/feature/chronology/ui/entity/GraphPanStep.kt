package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Один шаг камеры: куда она встала и сколько из запрошенного взяла.
 *
 * Потреблённое возвращается вместе с новой камерой, а не выводится из неё вызывающим: у границы
 * разница между запрошенным и взятым — единственный признак, по которому затухание понимает, что
 * дальше ехать некуда. Без него анимация доигрывает свою длительность целиком, кормя камеру за
 * границей.
 *
 * @param camera новый сдвиг содержимого, уже внутри диапазона
 * @param consumed часть запрошенной дельты, которую камера отработала
 */
@Immutable
data class GraphPanStep(
  val camera: Offset,
  val consumed: Offset
)
