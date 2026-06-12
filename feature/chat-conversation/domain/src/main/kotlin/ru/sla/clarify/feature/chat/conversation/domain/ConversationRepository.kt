package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.conversation.domain.entity.Group
import ru.sla.clarify.feature.chat.conversation.domain.entity.GroupMember
import ru.sla.clarify.feature.entity.chat.Peer

interface ConversationRepository {
  suspend fun subscribeOnConversations()
  suspend fun subscribeOnParticipantProfiles()
  suspend fun subscribeOnConversationsUnreadCounts()
  suspend fun subscribeOnGroupParticipants(id: Conversation.Id)

  suspend fun fetchCurrentUser()
  suspend fun getPeerByEmail(email: Email): Peer.Id
  suspend fun deleteConversations(ids: List<Conversation.Id>)

  suspend fun createGroup(name: String): Conversation.Id
  suspend fun renameGroup(id: Conversation.Id, name: String)
  suspend fun deleteGroup(id: Conversation.Id)
  suspend fun leaveGroup(id: Conversation.Id)
  suspend fun inviteGroupMembers(id: Conversation.Id, userIds: List<UserId>)
  suspend fun removeGroupMember(id: Conversation.Id, userId: UserId)
  suspend fun searchMemberByPrefix(prefix: String): List<FoundUser>

  val user: Flow<User?>

  val conversations: Flow<List<Conversation>?>

  fun observeGroup(id: Conversation.Id): Flow<Group?>
  fun observeGroupMembers(id: Conversation.Id): Flow<List<GroupMember>>
}
