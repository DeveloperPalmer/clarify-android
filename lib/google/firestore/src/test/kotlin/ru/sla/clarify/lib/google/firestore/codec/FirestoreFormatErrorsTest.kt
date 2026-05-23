package ru.sla.clarify.lib.google.firestore.codec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Контрактные тесты на ошибки схемы: decoder бросает [DataMappingException] вместо
 * NPE/ClassCastException, чтобы такие ошибки можно было отдельно отследить в крашлитике.
 */
class FirestoreFormatErrorsTest {

  private val format = FirestoreFormat.Default

  @Serializable
  private data class RequiredNM(val name: String, val count: Long)

  @Test
  fun `decode — missing required field throws DataMappingException`() {
    val source = mapOf<String, Any?>("name" to "x")
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<RequiredNM>(source)
    }
    assertTrue(ex.message!!.contains("count"), "сообщение должно ссылаться на поле 'count': ${ex.message}")
  }

  @Serializable
  private data class WithNestedNM(val name: String, val inner: InnerNM)

  @Serializable
  private data class InnerNM(val tag: String)

  @Test
  fun `decode — non-map for nested struct throws wrongType`() {
    val source = mapOf<String, Any?>("name" to "x", "inner" to "not-a-map")
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<WithNestedNM>(source)
    }
    assertTrue(ex.message!!.contains("expected Map"), "ожидалось 'expected Map' в сообщении: ${ex.message}")
  }

  @Serializable
  private data class WithListNM(val tags: List<String>)

  @Test
  fun `decode — non-list for list field throws wrongType`() {
    val source = mapOf<String, Any?>("tags" to "single")
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<WithListNM>(source)
    }
    assertTrue(ex.message!!.contains("expected List"))
  }

  @Serializable
  private enum class Status {
    @SerialName("active")
    ACTIVE,

    @SerialName("merged")
    MERGED
  }

  @Serializable
  private data class WithEnumNM(val status: Status)

  @Test
  fun `decode — unknown enum literal throws DataMappingException`() {
    val source = mapOf<String, Any?>("status" to "exploded")
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<WithEnumNM>(source)
    }
    assertTrue(ex.message!!.contains("exploded"))
  }

  @Test
  fun `decode — wrongType message mentions actual type`() {
    val source = mapOf<String, Any?>("name" to "x", "inner" to 42L)
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<WithNestedNM>(source)
    }
    // Сообщение должно включать "Long" — фактический тип.
    assertTrue(ex.message!!.contains("Long"), "actual-type 'Long' должен быть в сообщении: ${ex.message}")
  }

  @Test
  fun `decode — empty map for class with required field fails on first field`() {
    val ex = assertThrows(DataMappingException::class.java) {
      format.decodeFromMap<RequiredNM>(emptyMap())
    }
    assertEquals("missing required field 'name'", ex.message)
  }
}
