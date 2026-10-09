package ru.sla.clarify.database.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.entity.chat.Branch

class BranchIdConverterTest {

  @Test
  fun `id survives encode and decode`() {
    val id = Branch.Id("branch-1")

    val encoded = BranchIdConverter.encode(id)

    assertEquals(id, BranchIdConverter.decode(encoded))
  }
}
