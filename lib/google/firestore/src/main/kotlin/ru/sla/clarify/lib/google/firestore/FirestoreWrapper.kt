package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
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
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type
import javax.inject.Inject

@SingleIn(AppScope::class)
class FirestoreWrapper @Inject constructor() : FirestoreWrapperProvider {

  private val remoteDB = FirebaseFirestore.getInstance().apply {
    firestoreSettings = FirebaseFirestoreSettings.Builder()
      .build()
  }

  override fun writeBatch(): WriteBatch {
    return remoteDB.batch()
  }

  override fun <T> runTransaction(block: Transaction.Function<T>): Task<T> {
    return remoteDB.runTransaction(block)
  }

  override fun userDocumentRef(userId: UserId): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.USERS_COLLECTION)
      .document(userId.value)
  }

  override fun usersQuery(whereEqualTo: String): Query {
    return remoteDB
      .collection(FirestoreSchema.USERS_COLLECTION)
      .whereEqualTo(FirestoreSchema.USER_EMAIL, whereEqualTo)
  }

  override fun usersQueryByEmailPrefix(prefix: String, limit: Long): Query {
    return remoteDB
      .collection(FirestoreSchema.USERS_COLLECTION)
      .orderBy(FirestoreSchema.USER_EMAIL)
      .startAt(prefix)
      .endAt(prefix + "")
      .limit(limit)
  }

  override fun conversationCollectionRef(): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
  }

  override fun conversationsQuery(
    whereArrayContains: UserId
  ): Query {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .whereArrayContains(
        FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
        whereArrayContains.value
      )
  }

  override fun conversationsQuery(
    whereEqualTo: Type,
    whereArrayContains: UserId
  ): Query {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .whereEqualTo(
        FirestoreSchema.CONVERSATION_TYPE,
        whereEqualTo.value
      )
      .whereArrayContains(
        FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS,
        whereArrayContains.value
      )
  }

  override fun conversationDocumentRef(
    conversationId: String
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
  }

  override fun commitsCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.COMMITS_COLLECTION)
  }

  override fun unreadCommitsDocumentRef(
    conversationId: String,
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.UNREAD_COMMITS_COLLECTION)
      .document(userId.value)
  }

  override fun branchesCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.BRANCHES_COLLECTION)
  }

  override fun branchDocumentRef(
    conversationId: String,
    branchId: String
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.BRANCHES_COLLECTION)
      .document(branchId)
  }

  override fun branchUnreadCommitsDocumentRef(
    conversationId: String,
    branchId: String,
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.BRANCHES_COLLECTION)
      .document(branchId)
      .collection(FirestoreSchema.UNREAD_COMMITS_COLLECTION)
      .document(userId.value)
  }

  override fun participantsCollectionRef(
    conversationId: String
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.PARTICIPANTS_COLLECTION)
  }

  override fun participantDocumentRef(
    conversationId: String,
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId)
      .collection(FirestoreSchema.PARTICIPANTS_COLLECTION)
      .document(userId.value)
  }
}
