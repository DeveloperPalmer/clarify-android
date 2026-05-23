package ru.sla.clarify.lib.google.firestore.codec

import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

/**
 * Тесты на сериализацию sentinel-типов как обычных полей внутри @Serializable NM.
 * Sentinel'ы живут прямо в payload — codec должен подменять каждый на
 * соответствующий `FieldValue.*` через атомарный сериализатор.
 */
class FirestoreFormatSentinelTest {

  private val format = FirestoreFormat.Default

  @Serializable
  private data class WriteParamsNM(
    val displayName: String,
    @Contextual
    val updatedAt: ServerTimestamp = ServerTimestamp,
    @Contextual
    val mergeRequest: Delete = Delete,
    @Contextual
    val count: Increment = Increment(3L)
  )

  @Test
  fun `encode — ServerTimestamp подставляется как FieldValue serverTimestamp`() {
    val encoded = format.encodeToMap(
      WriteParamsNM(displayName = "Alice")
    )
    assertEquals("Alice", encoded["displayName"])
    assertSame(FieldValue.serverTimestamp(), encoded["updatedAt"])
  }

  @Test
  fun `encode — Delete подставляется как FieldValue delete`() {
    val encoded = format.encodeToMap(
      WriteParamsNM(displayName = "Alice")
    )
    assertSame(FieldValue.delete(), encoded["mergeRequest"])
  }

  @Test
  fun `encode — Increment подставляется как FieldValue increment`() {
    val encoded = format.encodeToMap(
      WriteParamsNM(displayName = "Alice")
    )
    // FieldValue.increment не singleton — проверяем тип и присутствие ключа.
    assertTrue(encoded["count"] is FieldValue, "ожидался FieldValue.increment")
  }

  @Test
  fun `decode — sentinel нельзя десериализовать (write-only)`() {
    // Симулируем входящий snapshot, у которого в этом поле что-то лежит. Decode
    // sentinel'ов намеренно запрещён — sentinel из снэпшота никогда не приходит,
    // в этом поле всегда уже резолвленное значение (Timestamp/Long/etc).
    val source = mapOf<String, Any?>(
      "displayName" to "Alice",
      "updatedAt" to "anything",
      "mergeRequest" to "anything",
      "count" to "anything"
    )
    assertThrows(SerializationException::class.java) {
      format.decodeFromMap<WriteParamsNM>(source)
    }
  }
}
