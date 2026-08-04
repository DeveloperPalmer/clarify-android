package ru.sla.clarify.database.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit

/**
 * Снапшот цитаты уезжает в колонку одной строкой, поэтому roundtrip проверяется целиком. Тест ловит
 * и потерю сгенерированного сериализатора: без плагина сериализации резолв уходит в рефлексию
 * и падает.
 */
class CommitReplyAdapterTest {

  @Test
  fun `reply survives encode and decode`() {
    val reply = Commit.Reply(
      id = Commit.Id("commit-1"),
      senderId = UserId("peer"),
      isSelf = false,
      text = "Мы \"цитируем\" текст со спецсимволами: {\"id\": 1}\nи переносом"
    )

    val encoded = CommitReplyAdapter.encode(reply)

    assertEquals(reply, CommitReplyAdapter.decode(encoded))
  }
}
