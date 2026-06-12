package ru.sla.clarify.feature.chat.conversation.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToUser
import ru.sla.clarify.feature.chat.conversation.data.mapper.selectAll
import ru.sla.clarify.feature.chat.conversation.data.mapper.selectAllGroupsAsConversations
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.PeerNotFoundException
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.conversation.domain.entity.Group
import ru.sla.clarify.feature.chat.conversation.domain.entity.GroupMember
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override suspend fun subscribeOnConversations() {
    firestore.conversationsLive()
      .flowOn(Dispatchers.IO)
      .collect(::applyConversationsChanges)
  }

  override suspend fun subscribeOnParticipantProfiles() {
    val userId = authSessionPersistence.withKey { readUserId(it) } ?: return
    persistedDB.chatConversationParticipantQueries
      .selectParticipantsWithoutProfile(userId.value)
      .observeList()
      .flowOn(Dispatchers.IO)
      .collect(::applyParticipantProfiles)
  }

  private suspend fun applyParticipantProfiles(ids: List<String>) = coroutineScope {
    ids.forEach { participantId ->
      launch { applyInsertOrReplaceUsers(participantId) }
    }
  }

  override suspend fun subscribeOnConversationsUnreadCounts() {
    persistedDB.chatConversationQueries
      .selectAllIds()
      .observeList()
      .flowOn(Dispatchers.IO)
      .collectLatest(::subscribeOnConversationsUnreadCounts)
  }

  private suspend fun subscribeOnConversationsUnreadCounts(ids: List<String>) = coroutineScope {
    ids.forEach { conversationId ->
      launch {
        firestore.unreadCountLive(
          conversationId = conversationId
        ).collect { unreadCount ->
          applyUpdateUnreadCount(
            conversationId = conversationId,
            unreadCount = unreadCount
          )
        }
      }
    }
  }

  override suspend fun fetchCurrentUser() {
    return withContext(Dispatchers.IO) {
      val firestoreUser = firestore.getCurrentUser()
      persistedDB.userQueries.insertOrReplace(
        id = firestoreUser.id,
        email = firestoreUser.email,
        displayName = firestoreUser.displayName,
        photoUrl = firestoreUser.photoUrl
      )
    }
  }

  override suspend fun getPeerByEmail(email: Email): Peer.Id {
    return withContext(Dispatchers.IO) {
      val userId = firestore.getUserIdByEmail(email) ?: throw PeerNotFoundException(email)
      Peer.Id(userId.value)
    }
  }

  override suspend fun createGroup(name: String): Conversation.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = firestore.postGroupConversation(name)
      Conversation.Id(conversationId)
    }
  }

  override suspend fun renameGroup(id: Conversation.Id, name: String) {
    withContext(Dispatchers.IO) {
      firestore.patchGroupName(conversationId = id.value, name = name)
      persistedDB.chatConversationQueries.updateGroupName(id = id.value, name = name)
    }
  }

  override suspend fun deleteGroup(id: Conversation.Id) {
    withContext(Dispatchers.IO) {
      firestore.deleteGroupConversation(id.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(id.value)
        persistedDB.chatConversationParticipantQueries.deleteByConversation(id.value)
      }
    }
  }

  override suspend fun leaveGroup(id: Conversation.Id) {
    withContext(Dispatchers.IO) {
      firestore.leaveGroup(id.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(id.value)
        persistedDB.chatConversationParticipantQueries.deleteByConversation(id.value)
      }
    }
  }

  override suspend fun inviteMembers(id: Conversation.Id, userIds: List<UserId>) {
    withContext(Dispatchers.IO) {
      userIds.forEach { userId ->
        firestore.postInviteParticipant(
          conversationId = id.value,
          invitedUserId = userId
        )
      }
    }
  }

  override suspend fun removeMember(id: Conversation.Id, userId: UserId) {
    withContext(Dispatchers.IO) {
      firestore.deleteParticipant(conversationId = id.value, userId = userId)
      persistedDB.chatConversationParticipantQueries.deleteByConversationAndId(
        conversationId = id.value,
        id = userId.value
      )
    }
  }

  override suspend fun searchUsersByEmailPrefix(prefix: String): List<FoundUser> {
    return withContext(Dispatchers.IO) {
      firestore.getUsersByEmailPrefix(prefix = prefix.lowercase(), limit = USER_SEARCH_LIMIT)
        .map { user ->
          FoundUser(
            id = UserId(user.id),
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl
          )
        }
    }
  }

  override suspend fun subscribeOnGroupParticipants(id: Conversation.Id) {
    firestore.participantsLive(id.value)
      .flowOn(Dispatchers.IO)
      .collect { changes ->
        persistedDB.transaction {
          changes.forEach { change ->
            when (change.changeType) {
              FirestoreDocumentResult.Added,
              FirestoreDocumentResult.Modified -> {
                persistedDB.chatConversationParticipantQueries.insertOrReplace(
                  conversationId = id.value,
                  id = change.data.id
                )
              }

              FirestoreDocumentResult.Removed -> {
                persistedDB.chatConversationParticipantQueries.deleteByConversationAndId(
                  conversationId = id.value,
                  id = change.data.id
                )
              }
            }
          }
        }
      }
  }

  override suspend fun deleteConversations(ids: List<Conversation.Id>) {
    return withContext(Dispatchers.IO) {
      val idValues = ids.map { it.value }
      firestore.deleteConversations(idValues)
      persistedDB.transaction {
        idValues.forEach {
          persistedDB.chatConversationQueries.deleteById(it)
          persistedDB.chatConversationParticipantQueries.deleteByConversation(it)
        }
      }
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)

    persistedDB.userQueries
      .selectById(userId.value, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val conversations: Flow<List<Conversation>> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(emptyList())

    val directs = persistedDB.chatConversationQueries
      .selectAll(userId)
      .observeList()
    val groups = persistedDB.chatConversationQueries
      .selectAllGroupsAsConversations()
      .observeList()
    combine(directs, groups) { d, g ->
      (d + g).sortedByDescending { it.lastCommitTimestamp }
    }.collect { emit(it) }
  }

  override fun observeGroup(id: Conversation.Id): Flow<Group?> {
    return persistedDB.chatConversationQueries
      .selectGroupById(
        id = id.value,
        mapper = { rowId, name, ownerUid, participantUids, _, _, _, _, memberCount ->
          Group(
            id = Conversation.Id(rowId),
            name = name.orEmpty(),
            ownerId = UserId(ownerUid.orEmpty()),
            memberCount = memberCount.toInt(),
            participantIds = participantUids.map(::UserId)
          )
        }
      )
      .observeOneOrNull()
  }

  override fun observeGroupMembers(id: Conversation.Id): Flow<List<GroupMember>> = flow {
    val currentUserId = authSessionPersistence.withKey { readUserId(it) }?.value
    val group = observeGroup(id)
    val members = persistedDB.chatConversationParticipantQueries
      .selectByConversation(
        conversationId = id.value,
        mapper = { memberId, displayName, photoUrl -> Triple(memberId, displayName, photoUrl) }
      )
      .observeList()
    combine(group, members) { groupValue, memberRows ->
      val ownerUid = groupValue?.ownerId?.value
      memberRows.map { (memberId, displayName, photoUrl) ->
        GroupMember(
          id = UserId(memberId),
          displayName = displayName,
          photoUrl = photoUrl,
          isOwner = memberId == ownerUid,
          isMe = memberId == currentUserId
        )
      }
    }.collect { emit(it) }
  }

  private fun applyConversationsChanges(changes: List<FirestoreChange<ConversationNM>>) {
    persistedDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Added -> {
            applyConversationChanges(change.data)
          }

          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceMetaConversation(change.data)
          }

          FirestoreDocumentResult.Removed -> {
            persistedDB.chatConversationQueries.deleteById(change.data.id)
            persistedDB.chatConversationParticipantQueries.deleteByConversation(change.data.id)
          }
        }
      }
    }
  }

  private fun applyConversationChanges(conversation: ConversationNM) {
    conversation.participantUids.forEach { participantId ->
      persistedDB.chatConversationParticipantQueries.insertOrReplace(
        conversationId = conversation.id,
        id = participantId
      )
    }
    persistedDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversation.id,
      type = conversation.type.value,
      participantUids = conversation.participantUids,
      name = conversation.name,
      ownerUid = conversation.ownerUid,
      lastCommit = conversation.lastCommitText,
      lastCommitSenderUid = conversation.lastCommitSenderUid,
      lastCommitTimestamp = conversation.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  private suspend fun applyInsertOrReplaceUsers(userId: String) {
    val profile = firestore.getUser(UserId(userId)) ?: return
    persistedDB.userQueries.insertOrReplace(
      id = profile.id,
      email = profile.email,
      displayName = profile.displayName,
      photoUrl = profile.photoUrl
    )
  }

  private fun applyInsertOrReplaceMetaConversation(conversationNM: ConversationNM) {
    persistedDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversationNM.id,
      type = conversationNM.type.value,
      participantUids = conversationNM.participantUids,
      name = conversationNM.name,
      ownerUid = conversationNM.ownerUid,
      lastCommit = conversationNM.lastCommitText,
      lastCommitSenderUid = conversationNM.lastCommitSenderUid,
      lastCommitTimestamp = conversationNM.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  private fun applyUpdateUnreadCount(conversationId: String, unreadCount: Long) {
    persistedDB.chatConversationQueries.updateUnreadCount(
      id = conversationId,
      unreadCount = unreadCount
    )
  }
}

private const val USER_SEARCH_LIMIT = 10L
