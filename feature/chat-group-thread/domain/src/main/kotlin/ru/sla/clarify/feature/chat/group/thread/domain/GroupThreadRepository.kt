package ru.sla.clarify.feature.chat.group.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.feature.chat.group.thread.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import java.time.LocalDateTime

interface GroupThreadRepository {
  suspend fun subscribeOnCommitChanges()
  suspend fun subscribeOnGroupMembers()

  suspend fun fetchHistoryCommits(count: Int, before: Commit? = null)
  suspend fun sendCommit(text: String)

  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  suspend fun renameGroup(name: String)
  suspend fun deleteConversation()
  suspend fun leaveConversation()
  suspend fun inviteGroupMembers(userIds: List<UserId>)
  suspend fun deleteConversationMember(userId: UserId)
  suspend fun searchMemberByPrefix(prefix: String): List<FoundUser>

  val commits: Flow<List<Commit>>
  val unreadCount: Flow<Long>

  fun observeGroup(): Flow<Group?>
  fun observeGroupMembers(): Flow<List<GroupMember>>
}
