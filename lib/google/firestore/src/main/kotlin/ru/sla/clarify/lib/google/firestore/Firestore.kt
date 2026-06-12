@file:Suppress("IgnoredReturnValue", "RedundantSuspendModifier")

package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.codec.FirestoreFormat
import ru.sla.clarify.lib.google.firestore.codec.decodeFromSnapshot
import ru.sla.clarify.lib.google.firestore.codec.encodeToMap
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM
import ru.sla.clarify.lib.google.firestore.entity.ParticipantNM
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchBranchOpenMergeParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchConversationLastCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchConversationNameParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchReadWatermarkParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadCountParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUnreadIncrementParams
import ru.sla.clarify.lib.google.firestore.entity.write.PatchUserParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostBranchParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostConversationParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostInviteParticipantCommitParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostParticipantParams
import ru.sla.clarify.lib.google.firestore.entity.write.PostUserParams
import ru.sla.clarify.lib.google.firestore.mapper.mapDocumentChanges
import ru.sla.clarify.lib.google.firestore.mapper.toFirestoreDocumentResult
import java.time.LocalDateTime
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
@SingleIn(AppScope::class)
class Firestore @Inject constructor(
  firestoreWrapper: FirestoreWrapper,
  private val authSessionPersistence: AuthSessionPersistence,
  private val listenerGuard: FirestoreListenerGuard
) : FirestoreWrapperProvider by firestoreWrapper {

  // Перед добавлением новых методов — прочитай соглашение об именовании.
  // root dir -> docs/firestore-naming-rules.md

  private val codec: FirestoreFormat = FirestoreFormat.Default

  /**
   * Создаёт `users/{uid}` с обоими server-stamp'ами (`createdAt`/`updatedAt`) и
   * payload-полями. Без `merge` — это первичная вставка. Вызывать **только**
   * когда документа ещё нет (проверяется через [isUserExists]).
   *
   * **Race window**: если два параллельных signIn одного аккаунта успели увидеть
   * `!isUserExists` до того как кто-то из них завершил `postUser`, оба запишут
   * документ — второй затрёт `createdAt` первого. Окно секундное, на login-flow
   * допустимо; полная атомарность потребовала бы Firestore-транзакции.
   */
  suspend fun postUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  ) {
    val payload = codec.encodeToMap(
      PostUserParams(
        displayName = displayName,
        photoUrl = photoUrl,
        email = email
      )
    )
    userDocumentRef(id)
      .set(payload)
      .await()
  }

  /**
   * Частичный апдейт `users/{uid}` через `set(merge)`. Семантика nullable-полей:
   * `null` означает «значение не присылали, существующее не трогаем» — поле не
   * попадает в map'у запроса (`@EncodeDefault(NEVER)` в [PatchUserParams]).
   * `updatedAt` всегда пишется server-stamp'ом.
   *
   * **Не покрывает осознанный clear**: чтобы удалить поле (например, аватарку)
   * — нужен отдельный метод с sentinel'ом
   * [ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete]; добавить когда
   * use case появится.
   */
  suspend fun patchUser(
    id: UserId,
    email: String,
    displayName: String,
    photoUrl: String?
  ) {
    val payload = codec.encodeToMap(
      PatchUserParams(
        displayName = displayName,
        photoUrl = photoUrl,
        email = email
      )
    )
    userDocumentRef(id)
      .set(payload, SetOptions.merge())
      .await()
  }

  suspend fun isUserExists(id: UserId): Boolean {
    return userDocumentRef(id)
      .get()
      .await()
      .exists()
  }

  suspend fun isUserExistsByEmail(email: Email): Boolean {
    return !usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
      .isEmpty
  }

  suspend fun getCurrentUser(): UserNM {
    val userId = requireUserId()
    val document = userDocumentRef(userId)
      .get()
      .await()

    if (!document.exists()) {
      error("User by id: ${userId.value} not found in Firestore")
    }

    return codec.decodeFromSnapshot<UserNM>(document)
  }

  suspend fun getUserIdByEmail(email: Email): UserId? {
    val snapshot = usersQuery(whereEqualTo = email.value.lowercase())
      .limit(1)
      .get()
      .await()
    return snapshot.documents.firstOrNull()
      ?.id
      ?.let(::UserId)
  }

  suspend fun getUser(id: UserId): UserNM? {
    val document = userDocumentRef(id)
      .get()
      .await()
    if (!document.exists()) return null
    return codec.decodeFromSnapshot<UserNM>(document)
  }

  fun userLive(id: UserId): Flow<UserNM?> {
    return callbackFlow {
      listenerGuard.trackOpen("userLive:${id.value}")

      val listener = userDocumentRef(id).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        val user = snapshot
          ?.takeIf { it.exists() }
          ?.let { codec.decodeFromSnapshot<UserNM>(it) }
        trySend(user)
      }

      awaitClose { listener.remove() }
    }
  }

  fun conversationsLive(): Flow<List<FirestoreChange<ConversationNM>>> {
    return callbackFlow {
      val userId = requireUserId()
      val listener = conversationsQuery(
        whereArrayContains = userId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        val response = snapshot.mapDocumentChanges { change ->
          FirestoreChange(
            data = codec.decodeFromSnapshot<ConversationNM>(change.document),
            changeType = change.type.toFirestoreDocumentResult()
          )
        }
        trySend(response)
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchClearUnreadCount(conversationId: String) {
    val userId = requireUserId()
    unreadCommitsDocumentRef(conversationId, userId)
      .set(
        codec.encodeToMap(PatchUnreadCountParams(count = 0L)),
        SetOptions.merge()
      )
      .await()
  }

  fun unreadCountLive(conversationId: String): Flow<Long> {
    return callbackFlow {
      val userId = requireUserId()
      val listener = unreadCommitsDocumentRef(
        conversationId = conversationId,
        userId = userId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.getLong(FirestoreSchema.UNREAD_COMMITS_COUNT) ?: 0L)
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchReadWatermark(
    conversationId: String,
    lastReadAt: LocalDateTime
  ) {
    val userId = requireUserId()
    participantDocumentRef(conversationId, userId)
      .set(
        codec.encodeToMap(PatchReadWatermarkParams(lastReadAt = lastReadAt.toTimestamp())),
        SetOptions.merge()
      )
      .await()
  }

  fun participantLive(
    conversationId: String,
    userId: UserId
  ): Flow<ParticipantNM?> {
    return callbackFlow {
      listenerGuard.trackOpen("participantLive:$conversationId:${userId.value}")

      val listener = participantDocumentRef(conversationId, userId)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val participant = snapshot
            ?.takeIf { it.exists() }
            ?.let { codec.decodeFromSnapshot<ParticipantNM>(it) }
          trySend(participant)
        }

      awaitClose { listener.remove() }
    }
  }

  /**
   * Создаёт групповой conversation. В отличие от direct'а (ленивая инициация при первом
   * сообщении) — группа материализуется сразу: документ + participant-документ для
   * создателя одной транзакцией. Подколлекции commits/branches/unreadCommits появляются
   * лениво при первом сообщении.
   */
  suspend fun postGroupConversation(name: String): String {
    val ownerId = requireUserId()
    val conversationId = randomUuid()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)
    batch.set(
      conversationRef,
      codec.encodeToMap(
        PostConversationParams(
          type = ConversationNM.Type.Group,
          participantUids = listOf(ownerId.value),
          name = name,
          ownerUid = ownerId.value
        )
      )
    )
    batch.set(
      participantDocumentRef(conversationId, ownerId),
      codec.encodeToMap(PostParticipantParams(id = ownerId.value))
    )
    batch.commit().await()
    return conversationId
  }

  suspend fun patchGroupName(conversationId: String, name: String) {
    conversationDocumentRef(conversationId)
      .set(
        codec.encodeToMap(PatchConversationNameParams(name = name)),
        SetOptions.merge()
      )
      .await()
  }

  /**
   * Приглашает пользователя в группу: одной транзакцией добавляет uid в `participantUids`
   * (arrayUnion), создаёт participant-документ и записывает системный commit типа
   * `inviteParticipant`. Такой commit НЕ обновляет `lastCommitText/lastCommitAt` и НЕ
   * инкрементит unread — поэтому только эти три записи в batch'е.
   */
  suspend fun postInviteParticipant(conversationId: String, invitedUserId: UserId) {
    val senderId = requireUserId()
    val createdAt = Timestamp.now()
    val commitId = randomUuid()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)
    batch.update(
      conversationRef,
      FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
      FieldValue.arrayUnion(invitedUserId.value)
    )
    batch.set(
      participantDocumentRef(conversationId, invitedUserId),
      codec.encodeToMap(PostParticipantParams(id = invitedUserId.value))
    )
    batch.set(
      conversationRef
        .collection(FirestoreSchema.COMMITS_COLLECTION)
        .document(commitId),
      codec.encodeToMap(
        PostInviteParticipantCommitParams(
          clientCommitId = commitId,
          senderUid = senderId,
          invitedUid = invitedUserId.value,
          branchId = conversationId,
          createdAt = createdAt
        )
      )
    )
    batch.commit().await()
  }

  /**
   * Удаляет участника из группы: arrayRemove из `participantUids` + удаление
   * participant-документа + unreadCommits-документа конкретного пользователя.
   * Остальные коллекции (commits, branches) остаются как есть.
   */
  suspend fun deleteParticipant(conversationId: String, userId: UserId) {
    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)
    batch.update(
      conversationRef,
      FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
      FieldValue.arrayRemove(userId.value)
    )
    batch.delete(participantDocumentRef(conversationId, userId))
    batch.delete(unreadCommitsDocumentRef(conversationId, userId))
    batch.commit().await()
  }

  suspend fun leaveGroup(conversationId: String) {
    deleteParticipant(conversationId, requireUserId())
  }

  fun participantsLive(conversationId: String): Flow<List<FirestoreChange<ParticipantNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("participantsLive:$conversationId")

      val listener = participantsCollectionRef(conversationId)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val response = snapshot.mapDocumentChanges { change ->
            FirestoreChange(
              changeType = change.type.toFirestoreDocumentResult(),
              data = codec.decodeFromSnapshot<ParticipantNM>(change.document)
            )
          }
          trySend(response)
        }

      awaitClose { listener.remove() }
    }
  }

  /**
   * Удаляет conversation-документ. Подколлекции commits/branches/participants/unreadCommits
   * остаются сиротами — осознанный технический долг MVP: чистка через server-side trigger /
   * recursive delete отложена.
   */
  suspend fun deleteGroupConversation(conversationId: String) {
    conversationDocumentRef(conversationId)
      .delete()
      .await()
  }

  /**
   * Prefix-поиск пользователей по email. email в users-документах нормализованы в
   * lowercase, поэтому caller должен передавать query тоже в lowercase. Запрос —
   * `orderBy(email).startAt(prefix).endAt(prefix + "")`, limit ограничивает
   * UI-список (по дизайну — 10).
   */
  suspend fun getUsersByEmailPrefix(prefix: String, limit: Long): List<UserNM> {
    return usersQueryByEmailPrefix(prefix = prefix, limit = limit)
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<UserNM>(it) }
  }

  suspend fun deleteConversations(ids: List<String>) {
    val batch = writeBatch()
    val reference = conversationCollectionRef()
    ids.forEach { id -> batch.delete(reference.document(id)) }
    batch.commit().await()
  }

  fun directCommitsLive(
    peerId: Peer.Id,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return directConversationIdLive(peerId)
      .distinctUntilChanged()
      .flatMapLatest { conversationId ->
        if (conversationId == null) {
          flowOf(emptyList())
        } else {
          conversationMessagesLive(
            id = conversationId,
            branchId = branchId,
            limit = limit
          )
        }
      }
  }

  suspend fun getCommits(
    conversationId: String,
    branchId: String,
    count: Int,
    before: LocalDateTime?
  ): List<CommitNM> {
    var query = commitsCollectionRef(conversationId)
      .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId)
      .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
      .limit(count.toLong())

    if (before != null) {
      query = query.whereLessThan(FirestoreSchema.COMMIT_CREATED_AT, before.toTimestamp())
    }

    return query
      .get()
      .await()
      .documents
      .map { codec.decodeFromSnapshot<CommitNM>(it) }
  }

  suspend fun postCommit(
    peerId: Peer.Id,
    branchId: String?,
    conversationId: String?,
    text: String,
    colorHex: String
  ) {
    val senderId = requireUserId()
    val isNewConversation = conversationId == null
    val conversationId = conversationId ?: randomUuid()
    // null branchId означает «писать в корень conversation» (master-ветка). У master-ветки
    // нет своего документа — её id совпадает с conversationId, — поэтому резолвим лениво
    // здесь, уже после того как conversationId мог быть сгенерирован.
    val resolvedBranchId = branchId ?: conversationId

    val commitId = randomUuid()
    val createdAt = Timestamp.now()
    val participantIds = directParticipantIds(senderId, peerId)
    val isRoot = resolvedBranchId == conversationId

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    val messageData = codec.encodeToMap(
      PostCommitParams(
        clientCommitId = commitId,
        senderUid = senderId,
        text = text,
        type = CommitNM.Type.Text,
        createdAt = createdAt,
        colorHex = colorHex,
        branchId = resolvedBranchId
      )
    )

    val conversationData = codec.encodeToMap(
      PostConversationParams(
        type = ConversationNM.Type.Direct,
        participantUids = participantIds,
        lastCommitText = text.takeIf { isRoot },
        lastCommitSenderUid = senderId.value.takeIf { isRoot },
        lastCommitAt = createdAt.takeIf { isRoot }
      )
    )

    val messageRef = conversationRef
      .collection(FirestoreSchema.COMMITS_COLLECTION)
      .document(commitId)

    batch.set(conversationRef, conversationData, SetOptions.merge())
    batch.set(messageRef, messageData)

    if (isNewConversation) {
      participantIds.forEach { uid ->
        batch.set(
          participantDocumentRef(conversationId, UserId(uid)),
          codec.encodeToMap(PostParticipantParams(id = uid))
        )
      }
    }

    if (isRoot) {
      participantIds
        .filter { it != senderId.value }
        .forEach { peerId ->
          batch.set(
            unreadCommitsDocumentRef(conversationId, UserId(peerId)),
            codec.encodeToMap(PatchUnreadIncrementParams()),
            SetOptions.merge()
          )
        }
    } else {
      batch.set(
        branchDocumentRef(conversationId, resolvedBranchId),
        codec.encodeToMap(
          PatchBranchLastCommitParams(
            lastCommitText = text,
            lastCommitAt = createdAt
          )
        ),
        SetOptions.merge()
      )
      participantIds
        .filter { it != senderId.value }
        .forEach { peerId ->
          batch.set(
            branchUnreadCommitsDocumentRef(conversationId, resolvedBranchId, UserId(peerId)),
            codec.encodeToMap(PatchUnreadIncrementParams()),
            SetOptions.merge()
          )
        }
    }

    batch.commit().await()
  }

  fun groupCommitsLive(
    conversationId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return conversationMessagesLive(
      id = conversationId,
      branchId = conversationId,
      limit = limit
    )
  }

  /**
   * Отправляет сообщение в группу. В отличие от direct-версии [postCommit] — conversation
   * уже существует, поэтому ни создания документа, ни participant-документов не нужно:
   * только commit + merge lastCommit-полей + unread-инкременты всем кроме отправителя.
   * [participantUids] передаёт caller (актуальный состав из локального кэша).
   */
  suspend fun postGroupCommit(
    conversationId: String,
    text: String,
    colorHex: String,
    participantUids: List<String>
  ) {
    val senderId = requireUserId()
    val commitId = randomUuid()
    val createdAt = Timestamp.now()

    val batch = writeBatch()
    val conversationRef = conversationDocumentRef(conversationId)

    batch.set(
      conversationRef
        .collection(FirestoreSchema.COMMITS_COLLECTION)
        .document(commitId),
      codec.encodeToMap(
        PostCommitParams(
          clientCommitId = commitId,
          senderUid = senderId,
          text = text,
          type = CommitNM.Type.Text,
          createdAt = createdAt,
          colorHex = colorHex,
          branchId = conversationId
        )
      )
    )
    batch.set(
      conversationRef,
      codec.encodeToMap(
        PatchConversationLastCommitParams(
          lastCommitText = text,
          lastCommitSenderUid = senderId.value,
          lastCommitAt = createdAt
        )
      ),
      SetOptions.merge()
    )
    participantUids
      .filter { it != senderId.value }
      .forEach { uid ->
        batch.set(
          unreadCommitsDocumentRef(conversationId, UserId(uid)),
          codec.encodeToMap(PatchUnreadIncrementParams()),
          SetOptions.merge()
        )
      }

    batch.commit().await()
  }

  fun branchesLive(
    conversationId: String
  ): Flow<List<FirestoreChange<BranchNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("branchesLive:$conversationId")

      val listener = branchesCollectionRef(
        conversationId = conversationId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        val response = snapshot.mapDocumentChanges { change ->
          FirestoreChange(
            changeType = change.type.toFirestoreDocumentResult(),
            data = codec.decodeFromSnapshot<BranchNM>(change.document)
          )
        }
        trySend(response)
      }

      awaitClose { listener.remove() }
    }
  }

  fun branchUnreadCountLive(
    conversationId: String,
    branchId: String
  ): Flow<Long> {
    return callbackFlow {
      val userId = requireUserId()
      val listener = branchUnreadCommitsDocumentRef(
        conversationId = conversationId,
        branchId = branchId,
        userId = userId
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.getLong(FirestoreSchema.UNREAD_COMMITS_COUNT) ?: 0L)
      }

      awaitClose { listener.remove() }
    }
  }

  suspend fun patchBranchClearUnreadCount(
    conversationId: String,
    branchId: String
  ) {
    val userId = requireUserId()
    branchUnreadCommitsDocumentRef(conversationId, branchId, userId)
      .set(
        codec.encodeToMap(PatchUnreadCountParams(count = 0L)),
        SetOptions.merge()
      )
      .await()
  }

  suspend fun postBranch(
    conversationId: String,
    parentBranchId: String,
    branchedFromCommitId: String,
    name: String
  ): BranchNM {
    val createdAt = Timestamp.now()
    val createdByUserId = requireUserId()

    val branchId = randomUuid()

    val branchData = codec.encodeToMap(
      PostBranchParams(
        parentBranchId = parentBranchId,
        branchedFromCommitId = branchedFromCommitId,
        name = name,
        createdAt = createdAt,
        createdByUid = createdByUserId
      )
    )

    branchDocumentRef(conversationId, branchId)
      .set(branchData)
      .await()

    return BranchNM(
      id = branchId,
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId,
      name = name,
      createdAt = createdAt,
      lastCommitAt = null,
      createdByUid = createdByUserId.value,
      mergeRequest = null
    )
  }

  /**
   * Открывает merge request для ветки. Атомарно: проходит только если у ветки сейчас
   * нет активного merge request'а. Записывает `mergeRequest = { status: Open, initiator,
   * requestedAt, approvedByUids: [] }`. Инициатор НЕ добавляется в approvers по
   * умолчанию — он должен явно нажать approve, как и все остальные участники.
   */
  suspend fun postMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val initiator = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val requestedAt = Timestamp.now()

    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      check(current.mergeRequest == null) {
        "Cannot open merge request: there is already an active one"
      }
      val update = codec.encodeToMap(
        PatchBranchOpenMergeParams(
          mergeRequest = MergeRequestNM(
            status = MergeRequestNM.Status.Open,
            initiatorUid = initiator.value,
            requestedAt = requestedAt,
            approvedByUids = emptyList()
          )
        )
      )
      txn.update(branchRef, update)
    }

    transaction.await()
  }

  /**
   * Добавляет текущего пользователя в список approvers активного merge request'а. Атомарно.
   *
   * Если с учётом текущего пользователя [participantUids] покрыт полностью, статус
   * переключается на [MergeRequestNM.Status.ReadyToMerge]. Финализацию merge'а делает
   * отдельный вызов [patchMergeFinalize].
   */
  suspend fun patchMergeApproval(
    conversationId: String,
    branchId: String,
    participantUids: List<String>
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot approve merge: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot approve merge: status is ${mergeRequest.status}"
      }

      val updatedApproved = (mergeRequest.approvedByUids + approver.value).distinct()

      val ready = participantUids.isNotEmpty() &&
        participantUids.toSet().subtract(updatedApproved.toSet()).isEmpty()

      // Partial dot-path update: трогаем только два поля внутри mergeRequest, а не
      // переписываем объект целиком — initiatorUid/requestedAt остаются нетронутыми.
      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
            to updatedApproved,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to (if (ready) MergeRequestNM.Status.ReadyToMerge else MergeRequestNM.Status.Open).value
        )
      )
    }

    transaction.await()
  }

  /**
   * Убирает текущего пользователя из списка approvers. Статус откатывается на
   * [MergeRequestNM.Status.Open] (на случай если был [MergeRequestNM.Status.ReadyToMerge]).
   */
  suspend fun deleteMergeApproval(
    conversationId: String,
    branchId: String
  ) {
    val approver = requireUserId()
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot revoke approval: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot revoke approval: status is ${mergeRequest.status}"
      }
      val updatedApproved = mergeRequest.approvedByUids.filter { it != approver.value }
      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS}"
            to updatedApproved,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to MergeRequestNM.Status.Open.value
        )
      )
    }

    transaction.await()
  }

  /**
   * Отменяет merge request — доступно любому участнику. Поле `mergeRequest` удаляется,
   * ветка снова принимает commit'ы.
   */
  suspend fun deleteMergeRequest(
    conversationId: String,
    branchId: String
  ) {
    val branchRef = branchDocumentRef(conversationId, branchId)
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot cancel merge: no active merge request")
      check(
        mergeRequest.status == MergeRequestNM.Status.Open ||
          mergeRequest.status == MergeRequestNM.Status.ReadyToMerge
      ) {
        "Cannot cancel merge: status is ${mergeRequest.status}"
      }
      txn.update(
        branchRef,
        mapOf(FirestoreSchema.BRANCH_MERGE_REQUEST to FieldValue.delete())
      )
    }

    transaction.await()
  }

  /**
   * Финализирует merge — доступно любому участнику. Требует статус
   * [MergeRequestNM.Status.ReadyToMerge]. Переводит merge request в
   * [MergeRequestNM.Status.Merged] и записывает `mergedAt = now`,
   * `mergedIntoBranchId = parentBranchId` внутрь mergeRequest.
   */
  suspend fun patchMergeFinalize(
    conversationId: String,
    branchId: String
  ) {
    val branchRef = branchDocumentRef(conversationId, branchId)
    val now = Timestamp.now()
    val transaction = runTransaction { txn ->
      val current = codec.decodeFromSnapshot<BranchNM>(txn.get(branchRef))
      val mergeRequest = current.mergeRequest
        ?: error("Cannot finalize merge: no active merge request")
      check(mergeRequest.status == MergeRequestNM.Status.ReadyToMerge) {
        "Cannot finalize merge: status is ${mergeRequest.status}"
      }
      txn.update(
        branchRef,
        mapOf(
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_STATUS}"
            to MergeRequestNM.Status.Merged.value,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_MERGED_AT}"
            to now,
          "${FirestoreSchema.BRANCH_MERGE_REQUEST}.${FirestoreSchema.BRANCH_MERGE_REQUEST_MERGED_INTO_BRANCH_ID}"
            to current.parentBranchId
        )
      )
    }

    transaction.await()
  }

  private fun directConversationIdLive(peerId: Peer.Id): Flow<String?> {
    return callbackFlow {
      val currentUid = requireUserId()
      listenerGuard.trackOpen("directConversationIdLive:${currentUid.value}:${peerId.value}")

      val directParticipantIds = directParticipantIds(currentUid, peerId)

      val listener = conversationsQuery(
        whereEqualTo = ConversationNM.Type.Direct,
        whereArrayContains = currentUid
      ).addSnapshotListener { snapshot, error ->
        if (error != null) {
          close(error)
          return@addSnapshotListener
        }
        trySend(snapshot?.findDirectConversationId(directParticipantIds))
      }

      awaitClose { listener.remove() }
    }
  }

  private fun conversationMessagesLive(
    id: String,
    branchId: String,
    limit: Long
  ): Flow<List<FirestoreChange<CommitNM>>> {
    return callbackFlow {
      listenerGuard.trackOpen("conversationMessagesLive:$id:$branchId")

      val listener = commitsCollectionRef(id)
        .whereEqualTo(FirestoreSchema.COMMIT_BRANCH_ID, branchId)
        .orderBy(FirestoreSchema.COMMIT_CREATED_AT, Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }
          val response = snapshot.mapDocumentChanges(MetadataChanges.INCLUDE) { change ->
            FirestoreChange(
              changeType = change.type.toFirestoreDocumentResult(),
              data = codec.decodeFromSnapshot<CommitNM>(change.document),
              hasPendingWrites = change.document.metadata.hasPendingWrites()
            )
          }
          trySend(response)
        }

      awaitClose { listener.remove() }
    }
  }

  private fun directParticipantIds(userId: UserId, peerId: Peer.Id): List<String> {
    return setOf(userId.value, peerId.value).sorted()
  }

  private fun QuerySnapshot?.findDirectConversationId(
    participantIds: List<String>
  ): String? {
    return this?.documents
      ?.firstOrNull { document ->
        codec.decodeFromSnapshot<ConversationNM>(document).participantUids == participantIds
      }
      ?.id
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) }) {
      "Current user not found in persistence"
    }
  }
}
