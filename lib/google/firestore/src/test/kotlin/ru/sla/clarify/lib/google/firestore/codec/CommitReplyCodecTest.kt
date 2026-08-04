package ru.sla.clarify.lib.google.firestore.codec

import com.google.firebase.Timestamp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ReplyCommitNM
import ru.sla.clarify.lib.google.firestore.entity.write.CreateCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.ReplyCommit

/**
 * Снапшот цитаты — вложенный объект документа коммита: проверяем и запись (params создания),
 * и чтение (NM), включая коммит без ответа.
 */
class CommitReplyCodecTest {

  private val format = FirestoreFormat.Default

  private val createdAt = Timestamp(1_700_000_000L, 0)

  private val replySnapshot = mapOf(
    "id" to "commit-1",
    "senderUid" to "peer",
    "text" to "hello"
  )

  @Test
  fun `reply snapshot is written as nested object`() {
    val params = createCommitParams(
      replyCommit = ReplyCommit(
        id = "commit-1",
        senderUid = UserId("peer"),
        text = "hello"
      )
    )

    assertEquals(replySnapshot, format.encodeToMap(params)["replyCommit"])
  }

  @Test
  fun `commit without reply writes no snapshot`() {
    assertNull(format.encodeToMap(createCommitParams(replyCommit = null))["replyCommit"])
  }

  @Test
  fun `reply snapshot is decoded back from the document`() {
    val document = commitDocument(replyCommit = replySnapshot)

    val commit = format.decodeFromMap<CommitNM>(document)

    assertEquals(
      ReplyCommitNM(
        id = "commit-1",
        senderUid = "peer",
        text = "hello"
      ),
      commit.replyCommit
    )
  }

  @Test
  fun `commit without reply is decoded with empty snapshot`() {
    val commit = format.decodeFromMap<CommitNM>(commitDocument(replyCommit = null))

    assertNull(commit.replyCommit)
  }

  private fun createCommitParams(replyCommit: ReplyCommit?): CreateCommitParams {
    return CreateCommitParams(
      senderUid = UserId("self"),
      text = "reply",
      type = CommitNM.Type.Text,
      createdAt = createdAt,
      branchId = "branch-1",
      visibleFor = listOf("self", "peer"),
      replyCommit = replyCommit
    )
  }

  private fun commitDocument(replyCommit: Map<String, Any?>?): Map<String, Any?> {
    return mapOf(
      "id" to "commit-2",
      "senderUid" to "self",
      "branchId" to "branch-1",
      "type" to "text",
      "text" to "reply",
      "visibleFor" to listOf("self", "peer"),
      "createdAt" to createdAt,
      "replyCommit" to replyCommit
    )
  }
}
