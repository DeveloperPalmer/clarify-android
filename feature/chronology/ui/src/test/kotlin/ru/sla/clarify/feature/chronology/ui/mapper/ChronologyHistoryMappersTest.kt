package ru.sla.clarify.feature.chronology.ui.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.Node
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chronology.domain.entity.BaselineHistory
import ru.sla.clarify.feature.chronology.domain.entity.BranchHistory
import ru.sla.clarify.feature.chronology.domain.entity.ChronologyHistory
import ru.sla.clarify.feature.chronology.ui.entity.BranchColor
import ru.sla.clarify.feature.chronology.ui.entity.Chronology
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import java.time.LocalDateTime
import java.time.ZoneId
import ru.sla.atlas.entity.Branch as GraphBranch

/**
 * Сторожит переходы, на которых граф врёт правдоподобно.
 *
 * Сборка выводит из ленты то, чего в ней прямо не записано: где кончается эпизод, куда встаёт
 * развилка, сколько непрочитанного приходится на плашку. Ошибка в любом из этих мест рисуется как
 * нормальный граф — узлы стоят, связи есть, — и на устройстве не отличается от правды ничем.
 */
class ChronologyHistoryMappersTest {

  @Test
  fun `nodes stand in the order of time, not in the order of branches`() {
    val chronology = historyOf(
      baselineCommits = listOf(messageOf(id = "t-1", at = at(hour = 12))),
      branches = listOf(
        BranchHistory(
          branch = branchOf(id = "b", createdAt = at(hour = 9)),
          commits = listOf(messageOf(id = "b-1", at = at(hour = 10)))
        )
      )
    ).toChronology()

    assertEquals(
      listOf("fork-b", "b-1", "t-1", "front"),
      chronology.graph.nodes.map { it.id.value },
      "ось X накапливается по порядку списка: ветка от десяти часов, поставленная после " +
        "магистрали от двенадцати, уехала бы на графе правее неё"
    )
  }

  @Test
  fun `a pause longer than three hours starts a new episode`() {
    val together = historyOf(
      baselineCommits = listOf(
        messageOf(id = "a", at = at(hour = 10)),
        messageOf(id = "b", at = at(hour = 12))
      )
    ).toChronology()
    val apart = historyOf(
      baselineCommits = listOf(
        messageOf(id = "a", at = at(hour = 10)),
        messageOf(id = "b", at = at(hour = 14))
      )
    ).toChronology()

    assertEquals(1, together.episodeCount(), "два часа — та же очередь реплик, а не второй эпизод")
    assertEquals(2, apart.episodeCount(), "четыре часа — уже разрыв: §5 брифа режет кластер по трём")
  }

  @Test
  fun `an episode is cut at twenty four messages`() {
    val minute = generateSequence(0) { it + 1 }.take(30).map { index ->
      messageOf(id = "m-$index", at = at(hour = 10).plusMinutes(index.toLong()))
    }.toList()

    val chronology = historyOf(baselineCommits = minute).toChronology()

    assertEquals(
      listOf(24, 6),
      chronology.episodes().map { it.count },
      "паузы между сообщениями нет вовсе, и без потолка §5 весь день стал бы одной плашкой"
    )
  }

  @Test
  fun `a system event stays out of the episode`() {
    val chronology = historyOf(
      baselineCommits = listOf(
        messageOf(id = "a", at = at(hour = 10), text = "последнее слово"),
        Commit.InviteMember(
          id = Commit.Id("invite"),
          timestamp = at(hour = 10).plusMinutes(1),
          senderId = UserId(SELF),
          isSelf = true,
          status = Commit.Status.Sent,
          invitedId = UserId(PEER)
        )
      )
    ).toChronology()

    val episode = chronology.episodes().single()
    assertEquals(1, episode.count, "приглашение участника — не реплика, и счётчик его не считает")
    assertEquals(
      "последнее слово",
      episode.text,
      "у приглашения нет текста, и попав в кластер, оно подменило бы сниппет пустой строкой"
    )
  }

  @Test
  fun `a merged branch gets its merge point and dims its episodes`() {
    val chronology = historyOf(
      baselineCommits = listOf(messageOf(id = "t-1", at = at(hour = 9))),
      branches = listOf(
        BranchHistory(
          branch = branchOf(
            id = "b",
            createdAt = at(hour = 10),
            mergeRequest = mergeRequestOf(
              status = Branch.MergeRequest.Status.Merged,
              mergedAt = at(hour = 18)
            )
          ),
          commits = listOf(messageOf(id = "b-1", at = at(hour = 11)))
        )
      )
    ).toChronology()

    val branch = chronology.graph.branches.single()
    assertEquals(GraphBranch.Status.Merged, branch.status)
    assertEquals(Node.Id("merge-b"), branch.mergedAt)
    assertTrue(
      chronology.graph.nodes.single { it.id == Node.Id("merge-b") } is GraphNode.Merge,
      "точка слияния обязана быть настоящим узлом списка, иначе её вертикаль пройдёт по чужой плашке"
    )
    assertTrue(
      chronology.episode("b-1").dim,
      "эпизоды закрытой темы рисуются приглушёнными — §6.5"
    )
    assertFalse(
      chronology.episode("t-1").dim,
      "приглушается ветка, а не всё, что случилось до её слияния"
    )
  }

  @Test
  fun `an open merge request puts no point on the baseline`() {
    val chronology = historyOf(
      branches = listOf(
        BranchHistory(
          branch = branchOf(
            id = "b",
            createdAt = at(hour = 10),
            mergeRequest = mergeRequestOf(status = Branch.MergeRequest.Status.Open)
          ),
          commits = listOf(messageOf(id = "b-1", at = at(hour = 11)))
        )
      )
    ).toChronology()

    assertEquals(GraphBranch.Status.Waiting, chronology.graph.branches.single().status)
    assertNull(
      chronology.graph.branches.single().mergedAt,
      "до финализации точки на магистрали нет вовсе: ветка заморожена, но не вернулась"
    )
    assertTrue(chronology.graph.nodes.none { it is GraphNode.Merge })
  }

  @Test
  fun `the fork stands on the message it left, not on the moment the branch was made`() {
    val chronology = historyOf(
      baselineCommits = listOf(
        messageOf(id = "old", at = at(hour = 9)),
        messageOf(id = "new", at = at(hour = 20))
      ),
      branches = listOf(
        BranchHistory(
          branch = branchOf(id = "b", createdAt = at(hour = 21), branchedFrom = "old"),
          commits = emptyList()
        )
      )
    ).toChronology()

    assertEquals(
      listOf("old", "fork-b", "new", "front"),
      chronology.graph.nodes.map { it.id.value },
      "разговор разошёлся на сообщении девяти часов, а не тогда, когда нажали кнопку"
    )
  }

  @Test
  fun `a branch whose source message is gone falls back to its creation time`() {
    val chronology = historyOf(
      baselineCommits = listOf(messageOf(id = "t-1", at = at(hour = 9))),
      branches = listOf(
        BranchHistory(
          branch = branchOf(id = "b", createdAt = at(hour = 20), branchedFrom = "paged-out"),
          commits = emptyList()
        )
      )
    ).toChronology()

    assertEquals(
      Node.Id("fork-b"),
      chronology.graph.branches.single().forkedFrom,
      "лента страничится с конца, и сообщение-развилка бывает ещё не догружено — ветка от этого " +
        "не перестаёт существовать"
    )
    assertEquals("fork-b", chronology.graph.nodes.last().id.value)
  }

  @Test
  fun `a branch with no anchor at all keeps its lane without a fork`() {
    val chronology = historyOf(
      branches = listOf(
        BranchHistory(
          branch = branchOf(id = "b", createdAt = null, branchedFrom = "paged-out"),
          commits = listOf(messageOf(id = "b-1", at = at(hour = 11)))
        )
      )
    ).toChronology()

    assertNull(chronology.graph.branches.single().forkedFrom)
    assertTrue(
      chronology.graph.nodes.none { it is GraphNode.Fork },
      "узел в нулевой секунде эпохи утянул бы начало графа в 1970 год и сжал бы историю в точку"
    )
  }

  @Test
  fun `unread is laid out from the last episode backwards`() {
    val chronology = historyOf(
      baselineCommits = listOf(
        messageOf(id = "a", at = at(hour = 9)),
        messageOf(id = "b", at = at(hour = 15)),
        messageOf(id = "c", at = at(hour = 15).plusMinutes(1)),
        messageOf(id = "d", at = at(hour = 21))
      ),
      baselineUnreadCount = 3
    ).toChronology()

    assertEquals(
      listOf(0L, 2L, 1L),
      chronology.episodes().map { it.unreadCount },
      "непрочитанными считаются последние N сообщений ленты, поэтому счёт идёт с конца"
    )
  }

  @Test
  fun `the front closes the baseline even when the last episode is a single message`() {
    val chronology = historyOf(
      baselineCommits = listOf(messageOf(id = "only", at = at(hour = 9)))
    ).toChronology()

    assertEquals(
      listOf(GraphNode.Episode::class, GraphNode.Front::class),
      chronology.graph.nodes.map { it::class },
      "фронт стоит на времени последнего сообщения, и на одиночном эпизоде обе величины совпадают"
    )
  }

  @Test
  fun `branch colours run in a circle and the baseline has none`() {
    val chronology = historyOf(
      branches = (1..7).map { order ->
        BranchHistory(
          branch = branchOf(id = "b-$order", createdAt = at(hour = 9).plusMinutes(order.toLong())),
          commits = emptyList()
        )
      }
    ).toChronology()

    assertEquals(
      listOf(
        BranchColor.First,
        BranchColor.Second,
        BranchColor.Third,
        BranchColor.Fourth,
        BranchColor.Fifth,
        BranchColor.Sixth,
        BranchColor.First
      ),
      (1..7).map { order -> chronology.branchColors.getValue(GraphBranch.Id("b-$order")) },
      "после последнего оттенка набор идёт по кругу: седьмая тема берёт первый"
    )
    assertNull(
      chronology.branchColors[GraphBranch.Id(BASELINE)],
      "у магистрали оттенка нет вовсе, и сказано это отсутствием, а не выделенным значением"
    )
  }

  @Test
  fun `an empty conversation gives an empty graph, not a broken one`() {
    val chronology = historyOf().toChronology()

    assertTrue(chronology.graph.nodes.isEmpty())
    assertTrue(chronology.graph.branches.isEmpty())
  }

  @Test
  fun `the episode carries the count, the last snippet and the share of my replies`() {
    val chronology = historyOf(
      baselineCommits = listOf(
        messageOf(id = "a", at = at(hour = 9), text = "первое", isSelf = true),
        messageOf(id = "b", at = at(hour = 9).plusMinutes(1), text = "второе", isSelf = true),
        messageOf(id = "c", at = at(hour = 9).plusMinutes(2), text = "последнее"),
        messageOf(id = "d", at = at(hour = 9).plusMinutes(3), text = "и правда последнее")
      ),
      members = listOf(Member(id = Member.Id(PEER), displayName = "Анна", photoUrl = null))
    ).toChronology()

    val episode = chronology.episodes().single()
    assertEquals(4, episode.count)
    assertEquals("и правда последнее", episode.text)
    assertEquals(0.5f, episode.myShare)

    assertEquals(
      "Анна",
      episode.preview.authorName,
      "карточка разворачивает последнее сообщение кластера"
    )
    assertEquals("и правда последнее", episode.preview.text)
  }
}

private fun Chronology.episodes(): List<GraphNode.Episode> {
  return graph.nodes.filterIsInstance<GraphNode.Episode>()
}

private fun Chronology.episodeCount(): Int {
  return episodes().size
}

/**
 * Эпизод по идентификатору его первого сообщения.
 *
 * @param id идентификатор узла
 * @return узел-эпизод; его отсутствие — ошибка теста, а не проверяемое значение
 */
private fun Chronology.episode(id: String): GraphNode.Episode {
  return episodes().single { it.id.value == id }
}

private const val BASELINE = "conversation"
private const val PEER = "peer"
private const val SELF = "self"

private fun at(hour: Int): LocalDateTime {
  return LocalDateTime.of(2026, 3, 6, hour, 0)
}

private fun LocalDateTime.toEpochSeconds(): Long {
  return atZone(ZoneId.systemDefault()).toEpochSecond()
}

private fun historyOf(
  baselineCommits: List<Commit> = emptyList(),
  baselineUnreadCount: Long = 0,
  branches: List<BranchHistory> = emptyList(),
  members: List<Member> = emptyList()
): ChronologyHistory {
  return ChronologyHistory(
    baseline = BaselineHistory(
      id = Branch.Id(BASELINE),
      commits = baselineCommits,
      unreadCount = baselineUnreadCount
    ),
    branches = branches,
    members = members
  )
}

private fun messageOf(
  id: String,
  at: LocalDateTime,
  text: String = "…",
  isSelf: Boolean = false
): Commit.Message {
  return Commit.Message(
    id = Commit.Id(id),
    timestamp = at,
    senderId = UserId(if (isSelf) SELF else PEER),
    isSelf = isSelf,
    status = Commit.Status.Sent,
    text = text
  )
}

private fun branchOf(
  id: String,
  createdAt: LocalDateTime?,
  branchedFrom: String = "",
  name: String = "Тема",
  unreadCount: Long = 0,
  mergeRequest: Branch.MergeRequest? = null
): Branch {
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(BASELINE),
    parentBranchId = Branch.Id(BASELINE),
    branchedFromCommitId = Commit.Id(branchedFrom),
    name = name,
    lastCommit = null,
    lastCommitAt = null,
    lastCommitTimestamp = 0,
    unreadCount = unreadCount,
    createdAt = createdAt?.toEpochSeconds() ?: 0,
    createdById = UserId(SELF),
    mergeRequest = mergeRequest
  )
}

private fun mergeRequestOf(
  status: Branch.MergeRequest.Status,
  mergedAt: LocalDateTime? = null
): Branch.MergeRequest {
  return Branch.MergeRequest(
    status = status,
    initiatorId = UserId(SELF),
    requestedAt = at(hour = 12).toEpochSeconds(),
    approvedByIds = emptySet(),
    mergedAt = mergedAt?.toEpochSeconds()
  )
}
