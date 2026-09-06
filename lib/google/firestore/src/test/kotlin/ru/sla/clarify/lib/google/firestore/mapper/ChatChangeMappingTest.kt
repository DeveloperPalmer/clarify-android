package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.firestore.DocumentChange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.entity.chat.ChatChange

/**
 * Сторож перевода изменений поставщика в доменные термины.
 *
 * Ветки по типу поставщика держит компилятор: `when` по enum обязан быть исчерпывающим и здесь,
 * и всюду, где репозитории разбирают [ChatChange.Type]. Не держит он обратное —
 * что каждый доменный тип кто-нибудь порождает. Добавленный в [ChatChange.Type] и разобранный
 * везде, где потребовал компилятор, новый тип может не прийти из поставщика ни разу за всю жизнь
 * приложения, и узнать об этом будет неоткуда: ветка есть, а данных в ней нет.
 */
class ChatChangeMappingTest {

  @Test
  fun `an added document becomes an added change`() {
    assertEquals(ChatChange.Type.Added, DocumentChange.Type.ADDED.toDomainModel())
  }

  @Test
  fun `every kind of change has a document change that produces it`() {
    assertEquals(
      ChatChange.Type.entries.toSet(),
      DocumentChange.Type.values().map { it.toDomainModel() }.toSet()
    )
  }
}
