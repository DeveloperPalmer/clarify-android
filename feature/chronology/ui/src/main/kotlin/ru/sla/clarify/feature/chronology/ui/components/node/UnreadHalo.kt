package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Мягкое свечение вокруг узла, в котором есть непрочитанное (§8 брифа).
 *
 * Живёт отдельно от узлов, потому что рисуют его все: эпизод, ветка, а дальше исток. Копия у каждого
 * разошлась бы с остальными молча — ни тест, ни компилятор о расхождении альфы или ширины не скажут,
 * а увидеть его можно только поставив два узла рядом, чего на полотне как раз не делают.
 *
 * Вызывается **до** того, как узлу задан размер: гало выходит за плашку и в раскладке места занимать
 * не должно, иначе непрочитанный узел оказался бы шире прочитанного и сдвигал бы соседей по дорожке.
 *
 * @param color цвет свечения, обычно акцент
 * @param cornerRadius скругление самой плашки: у эпизода и ветки `round16`, у узла покрупнее — своё
 */
internal fun DrawScope.drawUnreadHalo(color: Color, cornerRadius: Dp) {
  val spread = UNREAD_HALO_WIDTH.toPx()
  drawRoundRect(
    color = color.copy(alpha = 0.16f),
    topLeft = Offset(-spread, -spread),
    size = Size(size.width + spread * 2, size.height + spread * 2),
    // Скругление плашки плюс ширина гало: иначе кольцо срезало бы углы карточки.
    cornerRadius = CornerRadius((cornerRadius + UNREAD_HALO_WIDTH).toPx())
  )
}

/** Насколько гало выступает за плашку. Читают и отрисовка, и отступы в превью узлов. */
internal val UNREAD_HALO_WIDTH: Dp = 6.dp
