package ru.sla.clarify.lib.google.firestore.codec

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Тесты на @Contextual Timestamp: значение должно проходить «опаково» — то есть
 * в выходной map'е лежит **тот же** инстанс Timestamp, который Firestore SDK потом
 * запишет напрямую без сериализации. При decode — наоборот, Timestamp возвращается
 * из исходной map'ы без копирования.
 */
class FirestoreFormatTimestampTest {

  private val format = FirestoreFormat.Default

  @Serializable
  private data class WithTimestampNM(
    val name: String,
    @Contextual val createdAt: Timestamp,
    @Contextual val updatedAt: Timestamp?
  )

  @Test
  fun `roundtrip — non-null timestamp is opaque`() {
    val ts = Timestamp(1_700_000_000L, 0)
    val source = WithTimestampNM(name = "x", createdAt = ts, updatedAt = ts)

    val encoded = format.encodeToMap(source)
    assertSame(ts, encoded["createdAt"], "Timestamp должен пройти как тот же инстанс")
    assertSame(ts, encoded["updatedAt"])

    val decoded = format.decodeFromMap<WithTimestampNM>(encoded)
    assertEquals(source, decoded)
    assertSame(ts, decoded.createdAt)
  }

  @Test
  fun `roundtrip — nullable timestamp absent`() {
    val ts = Timestamp(1_700_000_000L, 500)
    val source = WithTimestampNM(name = "x", createdAt = ts, updatedAt = null)

    val encoded = format.encodeToMap(source)
    assertSame(ts, encoded["createdAt"])
    assertNull(encoded["updatedAt"])

    assertEquals(source, format.decodeFromMap<WithTimestampNM>(encoded))
  }
}
