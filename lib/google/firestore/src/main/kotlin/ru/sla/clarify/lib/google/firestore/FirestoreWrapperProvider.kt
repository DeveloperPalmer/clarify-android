package ru.sla.clarify.lib.google.firestore

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.lib.google.firestore.entity.CommitCursor
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type

interface FirestoreWrapperProvider {
  val emptyMap: Map<String, Any>

  fun writeBatch(): WriteBatch
  fun <T> runTransaction(block: Transaction.Function<T>): Task<T>

  fun userDocumentRef(userId: UserId): DocumentReference

  fun usersQuery(whereEqualTo: String): Query

  fun usersQueryByEmailPrefix(prefix: String, limit: Long): Query

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

  fun commitQuery(
    conversationId: String,
    whereEqualTo: Branch.Id,
    whereArrayContains: UserId,
    before: CommitCursor?,
    limit: Long
  ): Query

  fun commitTailQuery(
    conversationId: String,
    whereEqualTo: Branch.Id,
    whereArrayContains: UserId,
    from: CommitCursor?
  ): Query

  fun unreadCommitsDocumentRef(
    conversationId: String,
    memberId: String
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
    memberId: String
  ): DocumentReference

  fun membersCollectionRef(
    conversationId: String
  ): CollectionReference

  fun memberDocumentRef(
    conversationId: String,
    memberId: String
  ): DocumentReference
}
