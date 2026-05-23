package ru.sla.clarify.lib.google.firestore.codec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * Тесты на специфику kotlinx.serialization: @SerialName для полей и enum-литералов,
 * default-значения у optional-полей (пропуск отсутствующего ключа в decoder'е).
 */
class FirestoreFormatEnumAndAliasTest {

  private val format = FirestoreFormat.Default

  @Serializable
  private enum class BranchStatus {
    @SerialName("active")
    ACTIVE,

    @SerialName("merged")
    MERGED,

    @SerialName("abandoned")
    ABANDONED
  }

  @Serializable
  private data class WithEnumNM(val status: BranchStatus)

  @Test
  fun `roundtrip — enum with @SerialName uses serial value`() {
    val encoded = format.encodeToMap(WithEnumNM(BranchStatus.MERGED))
    assertEquals(mapOf("status" to "merged"), encoded)
    assertEquals(WithEnumNM(BranchStatus.MERGED), format.decodeFromMap<WithEnumNM>(encoded))
  }

  @Serializable
  private data class RenamedNM(
    @SerialName("created_at") val createdAt: String,
    @SerialName("created_by_uid") val createdByUid: String
  )

  @Test
  fun `roundtrip — @SerialName on property uses aliased key`() {
    val source = RenamedNM(createdAt = "T0", createdByUid = "u1")
    val encoded = format.encodeToMap(source)
    assertEquals(mapOf("created_at" to "T0", "created_by_uid" to "u1"), encoded)
    assertFalse(encoded.containsKey("createdAt"))
    assertEquals(source, format.decodeFromMap<RenamedNM>(encoded))
  }

  @Serializable
  private data class WithDefaultsNM(
    val required: String,
    val optionalString: String = "default-value",
    val optionalNullable: String? = null,
    val optionalList: List<String> = emptyList()
  )

  @Test
  fun `decode — optional fields take defaults when key absent`() {
    val source = mapOf("required" to "r")
    val decoded = format.decodeFromMap<WithDefaultsNM>(source)
    assertEquals(
      WithDefaultsNM(
        required = "r",
        optionalString = "default-value",
        optionalNullable = null,
        optionalList = emptyList()
      ),
      decoded
    )
  }

  @Test
  fun `decode — optional fields override defaults when key present`() {
    val source = mapOf(
      "required" to "r",
      "optionalString" to "explicit",
      "optionalNullable" to "value",
      "optionalList" to listOf("a", "b")
    )
    val decoded = format.decodeFromMap<WithDefaultsNM>(source)
    assertEquals(
      WithDefaultsNM(
        required = "r",
        optionalString = "explicit",
        optionalNullable = "value",
        optionalList = listOf("a", "b")
      ),
      decoded
    )
  }
}
