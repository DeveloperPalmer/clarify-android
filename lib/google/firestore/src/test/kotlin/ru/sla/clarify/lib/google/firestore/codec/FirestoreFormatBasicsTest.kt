package ru.sla.clarify.lib.google.firestore.codec

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Roundtrip-тесты ядра кодека на простых формах: примитивы, nullable, nested, list.
 * Цель — убедиться, что `FirestoreFormat.encodeToMap` / `decodeFromMap` сохраняют
 * структуру документа без потерь и без участия Firestore SDK.
 */
class FirestoreFormatBasicsTest {

  private val format = FirestoreFormat.Default

  @Serializable
  private data class PrimitivesNM(
    val name: String,
    val count: Long,
    val ratio: Double,
    val active: Boolean
  )

  @Test
  fun `roundtrip — primitive types`() {
    val source = PrimitivesNM(name = "alpha", count = 42L, ratio = 1.5, active = true)
    val encoded = format.encodeToMap(source)

    assertEquals(
      mapOf("name" to "alpha", "count" to 42L, "ratio" to 1.5, "active" to true),
      encoded
    )
    assertEquals(source, format.decodeFromMap<PrimitivesNM>(encoded))
  }

  @Serializable
  private data class NullableNM(
    val required: String,
    val nullable: String?
  )

  @Test
  fun `roundtrip — nullable present`() {
    val source = NullableNM(required = "r", nullable = "n")
    val encoded = format.encodeToMap(source)
    assertEquals(mapOf("required" to "r", "nullable" to "n"), encoded)
    assertEquals(source, format.decodeFromMap<NullableNM>(encoded))
  }

  @Test
  fun `roundtrip — nullable absent encodes to null`() {
    val source = NullableNM(required = "r", nullable = null)
    val encoded = format.encodeToMap(source)

    // Encoder выписывает null в map: ключ присутствует, значение null.
    assertTrue(encoded.containsKey("nullable"), "ключ 'nullable' должен присутствовать")
    assertNull(encoded["nullable"])
    assertEquals(source, format.decodeFromMap<NullableNM>(encoded))
  }

  @Serializable
  private data class InnerNM(val tag: String, val number: Long)

  @Serializable
  private data class OuterNM(
    val title: String,
    val inner: InnerNM,
    val nullableInner: InnerNM?
  )

  @Test
  fun `roundtrip — nested data class`() {
    val source = OuterNM(
      title = "T",
      inner = InnerNM(tag = "x", number = 7L),
      nullableInner = InnerNM(tag = "y", number = 8L)
    )
    val encoded = format.encodeToMap(source)

    assertEquals(
      mapOf(
        "title" to "T",
        "inner" to mapOf("tag" to "x", "number" to 7L),
        "nullableInner" to mapOf("tag" to "y", "number" to 8L)
      ),
      encoded
    )
    assertEquals(source, format.decodeFromMap<OuterNM>(encoded))
  }

  @Test
  fun `roundtrip — nested nullable absent`() {
    val source = OuterNM(
      title = "T",
      inner = InnerNM(tag = "x", number = 7L),
      nullableInner = null
    )
    val encoded = format.encodeToMap(source)
    assertNull(encoded["nullableInner"])
    assertEquals(source, format.decodeFromMap<OuterNM>(encoded))
  }

  @Serializable
  private data class ListsNM(
    val strings: List<String>,
    val numbers: List<Long>,
    val nested: List<InnerNM>
  )

  @Test
  fun `roundtrip — lists of primitives and nested`() {
    val source = ListsNM(
      strings = listOf("a", "b", "c"),
      numbers = listOf(1L, 2L, 3L),
      nested = listOf(InnerNM("x", 1L), InnerNM("y", 2L))
    )
    val encoded = format.encodeToMap(source)

    assertEquals(listOf("a", "b", "c"), encoded["strings"])
    assertEquals(listOf(1L, 2L, 3L), encoded["numbers"])
    assertEquals(
      listOf(
        mapOf("tag" to "x", "number" to 1L),
        mapOf("tag" to "y", "number" to 2L)
      ),
      encoded["nested"]
    )
    assertEquals(source, format.decodeFromMap<ListsNM>(encoded))
  }

  @Test
  fun `roundtrip — empty list`() {
    val source = ListsNM(strings = emptyList(), numbers = emptyList(), nested = emptyList())
    val encoded = format.encodeToMap(source)
    assertEquals(emptyList<Any?>(), encoded["strings"])
    assertEquals(source, format.decodeFromMap<ListsNM>(encoded))
  }
}
