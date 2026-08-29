package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Где полотно стояло до того, как его вписали целиком: куда возвращает повторный двойной тап (§11.1).
 *
 * Три величины держатся вместе, а не порознь, по той же причине, по которой вместе подменяются узлы
 * и ветки: они описывают одно положение, и восстановленные по отдельности дали бы камеру одного
 * уровня в масштабе другого.
 *
 * Признак «камеру трогали» входит сюда наравне с остальными: без него возврат к нетронутой камере
 * закреплял бы её, и покой, следующий за новыми узлами, был бы потерян молча.
 *
 * @param level уровень детализации до вписывания
 * @param scale масштаб до вписывания
 * @param camera хранимый сдвиг содержимого; при нетронутой камере он не читается вовсе
 * @param isMoved трогали ли камеру до вписывания
 */
@Immutable
data class GraphCameraPose(
  val level: GraphLevel,
  val scale: Float,
  val camera: Offset,
  val isMoved: Boolean
)
