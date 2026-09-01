package ru.sla.atlas.debug

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/*
 * Оформление панели: своё, минимальное и намеренно чужое остальному приложению.
 *
 * Панель — инструмент разработчика, а не часть экрана, и брать её вид из дизайн-системы значило бы
 * тянуть в библиотеку зависимость ради восьми цветов. Своя палитра заодно делает панель узнаваемой:
 * она обязана быть видна поверх любого полотна, какой бы темы оно ни держалось.
 */

/** Подложка панели: почти чёрная и почти непрозрачная — граф под ней читаться не должен. */
internal val PANEL_BACKGROUND = Color(0xF2101614)

/** Подпись метрики. */
internal val LABEL_COLOR = Color(0xFF8FA09A)

/** Значение метрики. */
internal val VALUE_COLOR = Color(0xFFE2E8E4)

/** Заливка строки, вышедшей за норму. */
internal val ANOMALY_BACKGROUND = Color(0xFF8C2F3F)

/** Содержимое строки, вышедшей за норму. */
internal val ANOMALY_CONTENT = Color(0xFFFFE3E8)

/** Строка панели: мелкая настолько, чтобы шестнадцать метрик не закрыли то, ради чего их открыли. */
internal val ROW_STYLE = TextStyle(fontSize = 11.sp, lineHeight = 14.sp)

/** Заголовок страницы. */
internal val TITLE_STYLE = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.08.sp)
