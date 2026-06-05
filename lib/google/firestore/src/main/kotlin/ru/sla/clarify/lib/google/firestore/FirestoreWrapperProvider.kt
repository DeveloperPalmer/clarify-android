package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type

interface FirestoreWrapperProvider {

  fun writeBatch(): WriteBatch
  fun <T> runTransaction(block: Transaction.Function<T>): Task<T>

  fun userDocumentRef(userId: UserId): DocumentReference

  fun usersQuery(whereEqualTo: String): Query

  fun conversationCollectionRef(): CollectionReference

  fun conversationsQuery(
    whereArrayContains: UserId
  ): Query

  fun conversationsQuery(
    whereEqualTo: Type,
    whereArrayContains: UserId
  ): Query

  fun conversationDocumentRef(
    conversationId: String
  ): DocumentReference

  fun commitsCollectionRef(
    conversationId: String
  ): CollectionReference

  fun unreadCommitsDocumentRef(
    conversationId: String,
    userId: UserId
  ): DocumentReference

  fun branchesCollectionRef(
    conversationId: String
  ): CollectionReference

  fun branchDocumentRef(
    conversationId: String,
    branchId: String
  ): DocumentReference

  fun branchUnreadCommitsDocumentRef(
    conversationId: String,
    branchId: String,
    userId: UserId
  ): DocumentReference

  fun participantsCollectionRef(
    conversationId: String
  ): CollectionReference

  fun participantDocumentRef(
    conversationId: String,
    userId: UserId
  ): DocumentReference
}
