package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation

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
    whereEqualTo: ConversationType,
    whereArrayContains: UserId
  ): Query

  fun conversationDocumentRef(
    conversationId: FirestoreConversation.Id
  ): DocumentReference

  fun commitsCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference

  fun unreadCommitsDocumentRef(
    conversationId: FirestoreConversation.Id,
    userId: UserId
  ): DocumentReference

  fun branchesCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference

  fun branchDocumentRef(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id
  ): DocumentReference

  fun participantsCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference

  fun participantDocumentRef(
    conversationId: FirestoreConversation.Id,
    userId: UserId
  ): DocumentReference
}
