package ru.sla.clarify.uikit.component.chat

/**
 * Маркер для коммитов, несущих текст. Намеренно НЕ наследует [Commit]: подтип sealed-интерфейса
 * с `@optics` ломает кодогенерацию призм. Текст достаётся кастом `commit as? Textual`, который
 * возвращает `null` для вариантов без текста.
 */
interface Textual {
  val text: String
}
