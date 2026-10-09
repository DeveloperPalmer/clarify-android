package ru.sla.clarify.database.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.entity.chat.Member

class MemberIdConverterTest {

  @Test
  fun `id survives encode and decode`() {
    val id = Member.Id("member-1")

    val encoded = MemberIdConverter.encode(id)

    assertEquals(id, MemberIdConverter.decode(encoded))
  }
}
