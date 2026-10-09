package ru.sla.clarify.database.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId

class UserIdSetConverterTest {

  @Test
  fun `ids survive encode and decode`() {
    val ids = setOf(UserId("alice"), UserId("bob"))

    val encoded = UserIdSetConverter.encode(ids)

    assertEquals(ids, UserIdSetConverter.decode(encoded))
  }

  @Test
  fun `empty set survives encode and decode`() {
    val encoded = UserIdSetConverter.encode(emptySet())

    assertEquals(emptySet<UserId>(), UserIdSetConverter.decode(encoded))
  }
}
