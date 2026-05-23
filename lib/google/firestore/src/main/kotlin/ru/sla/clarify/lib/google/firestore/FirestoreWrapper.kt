package ru.sla.clarify.lib.google.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import javax.inject.Inject

@SingleIn(AppScope::class)
class FirestoreWrapper @Inject constructor() : FirestoreWrapperProvider {

  override val remoteDB = FirebaseFirestore.getInstance().apply {
    firestoreSettings = FirebaseFirestoreSettings.Builder()
      .build()
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
    whereEqualTo: ConversationType,
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
    conversationId: FirestoreConversation.Id
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
  }

  override fun commitsCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.COMMITS_COLLECTION)
  }

  override fun unreadCommitsDocumentRef(
    conversationId: FirestoreConversation.Id,
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.UNREAD_COMMITS_COLLECTION)
      .document(userId.value)
  }

  override fun branchesCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.BRANCHES_COLLECTION)
  }

  override fun branchDocumentRef(
    conversationId: FirestoreConversation.Id,
    branchId: FirestoreBranch.Id
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.BRANCHES_COLLECTION)
      .document(branchId.value)
  }

  override fun participantsCollectionRef(
    conversationId: FirestoreConversation.Id
  ): CollectionReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.PARTICIPANTS_COLLECTION)
  }

  override fun participantDocumentRef(
    conversationId: FirestoreConversation.Id,
    userId: UserId
  ): DocumentReference {
    return remoteDB
      .collection(FirestoreSchema.CONVERSATIONS_COLLECTION)
      .document(conversationId.value)
      .collection(FirestoreSchema.PARTICIPANTS_COLLECTION)
      .document(userId.value)
  }
}
