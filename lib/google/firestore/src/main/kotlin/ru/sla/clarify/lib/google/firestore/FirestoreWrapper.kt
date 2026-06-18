package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCHES_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMITS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_BRANCH_ID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_CREATED_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATIONS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_MEMBER_UIDS
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_TYPE
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.MEMBERS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.UNREAD_COMMITS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.USERS_COLLECTION
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.USER_EMAIL
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type
import javax.inject.Inject

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
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(UNREAD_COMMITS_COLLECTION)
      .document(userId.value)
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
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(BRANCHES_COLLECTION)
      .document(branchId)
      .collection(UNREAD_COMMITS_COLLECTION)
      .document(userId.value)
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
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(MEMBERS_COLLECTION)
      .document(userId.value)
  }

  override fun commitQuery(
    conversationId: String,
    whereEqualTo: Branch.Id,
    before: Timestamp?,
    limit: Long
  ): Query {
    return remoteDB
      .collection(CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(COMMITS_COLLECTION)
      .limit(limit)
      .whereEqualTo(COMMIT_BRANCH_ID, whereEqualTo.value)
      .let { if (before != null) it.whereLessThan(COMMIT_CREATED_AT, before) else it }
      .orderBy(COMMIT_CREATED_AT, Query.Direction.DESCENDING)
  }
}
