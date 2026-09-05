package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.CommitCursor
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCHES_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMITS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_BRANCH_ID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_CREATED_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_VISIBLE_FOR
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATIONS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_MEMBER_UIDS
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_TYPE
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.MEMBERS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.UNREAD_COMMITS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.USERS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.USER_EMAIL
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
class FirestoreWrapper @Inject constructor() : FirestoreWrapperProvider {

  private val remoteDB = FirebaseFirestore.getInstance().apply {
    firestoreSettings = FirebaseFirestoreSettings.Builder()
      .build()
  }

  override val emptyMap: Map<String, Any> = emptyMap()

  override fun writeBatch(): WriteBatch {
    return remoteDB.batch()
  }

  override fun <T> runTransaction(block: Transaction.Function<T>): Task<T> {
    return remoteDB.runTransaction(block)
  }

  override fun userDocumentRef(userId: UserId): DocumentReference {
    return remoteDB
      .collection(USERS_COLLECTION)
      .document(userId.value)
  }

  override fun usersQuery(whereEqualTo: String): Query {
    return remoteDB
      .collection(USERS_COLLECTION)
      .whereEqualTo(USER_EMAIL, whereEqualTo)
  }

  override fun usersQueryByEmailPrefix(prefix: String, limit: Long): Query {
    return remoteDB
      .collection(USERS_COLLECTION)
      .orderBy(USER_EMAIL)
      .startAt(prefix)
      .endAt("$prefix\uF8FF")
      .limit(limit)
  }

  override fun conversationCollectionRef(): CollectionReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
  }

  override fun conversationsQuery(
    whereArrayContains: UserId
  ): Query {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .whereArrayContains(CONVERSATION_MEMBER_UIDS, whereArrayContains.value)
  }

  override fun conversationsQuery(
    whereEqualTo: Type,
    whereArrayContains: UserId
  ): Query {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .whereEqualTo(CONVERSATION_TYPE, whereEqualTo.value)
      .whereArrayContains(CONVERSATION_MEMBER_UIDS, whereArrayContains.value)
  }

  override fun conversationDocumentRef(
    conversationId: String
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
  }

  override fun commitsCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(COMMITS_COLLECTION)
  }

  override fun unreadCommitsDocumentRef(
    conversationId: String,
    memberId: String
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(UNREAD_COMMITS_COLLECTION)
      .document(memberId)
  }

  override fun branchesCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(BRANCHES_COLLECTION)
  }

  override fun branchDocumentRef(
    conversationId: String,
    branchId: String
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(BRANCHES_COLLECTION)
      .document(branchId)
  }

  override fun branchUnreadCommitsDocumentRef(
    conversationId: String,
    branchId: String,
    memberId: String
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(BRANCHES_COLLECTION)
      .document(branchId)
      .collection(UNREAD_COMMITS_COLLECTION)
      .document(memberId)
  }

  override fun membersCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(MEMBERS_COLLECTION)
  }

  override fun memberDocumentRef(
    conversationId: String,
    memberId: String
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(MEMBERS_COLLECTION)
      .document(memberId)
  }

  override fun commitQuery(
    conversationId: String,
    whereEqualTo: Branch.Id,
    whereArrayContains: UserId,
    before: CommitCursor?,
    limit: Long
  ): Query {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(COMMITS_COLLECTION)
      .whereEqualTo(COMMIT_BRANCH_ID, whereEqualTo.value)
      .whereArrayContains(COMMIT_VISIBLE_FOR, whereArrayContains.value)
      // documentId() — тай-брейк сортировки: делает курсор (createdAt, id) точным, чтобы
      // пагинация не пропускала и не дублировала коммиты с одинаковым createdAt. То же направление,
      // что и у createdAt, поэтому Firestore обслуживает запрос из существующего composite-индекса
      // (неявный __name__).
      .orderBy(COMMIT_CREATED_AT, Query.Direction.DESCENDING)
      .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
      .let {
        if (before != null) {
          it.startAfter(before.createdAtNanos.epochNanosToTimestamp(), before.id.value)
        } else {
          it
        }
      }
      .limit(limit)
  }

  override fun commitTailQuery(
    conversationId: String,
    whereEqualTo: Branch.Id,
    whereArrayContains: UserId,
    from: CommitCursor?
  ): Query {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(COMMITS_COLLECTION)
      .whereEqualTo(COMMIT_BRANCH_ID, whereEqualTo.value)
      .whereArrayContains(COMMIT_VISIBLE_FOR, whereArrayContains.value)
      // Восходящее зеркало commitQuery: forward-«tail» от самого нового закэшированного коммита.
      // Требует собственный composite-индекс (visibleFor array-contains, branchId, createdAt ASC,
      // __name__ ASC) — arrayContains + несколько orderBy НЕ обслуживаются разворотом DESC-индекса
      // пагинации. Без limit: верхняя граница вытесняла бы старые строки из окна и порождала
      // фантомные REMOVED-изменения; нижняя граница-курсор одна удерживает слушатель на новых коммитах.
      .orderBy(COMMIT_CREATED_AT, Query.Direction.ASCENDING)
      .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
      .let {
        if (from != null) {
          // Включительно (startAt, а не startAfter): граничный коммит сам остаётся в окне,
          // поэтому его последующие правки/удаления по-прежнему наблюдаются вживую. Его повторная
          // выдача как ADDED — идемпотентный insertOrReplace.
          it.startAt(from.createdAtNanos.epochNanosToTimestamp(), from.id.value)
        } else {
          it
        }
      }
  }
}
