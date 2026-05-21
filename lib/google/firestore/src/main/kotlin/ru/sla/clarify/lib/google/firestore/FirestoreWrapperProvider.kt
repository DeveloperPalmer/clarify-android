package ru.sla.clarify.lib.google.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation

interface FirestoreWrapperProvider {
  val remoteDB: FirebaseFirestore

  fun userDocumentRef(userId: UserId): DocumentReference

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
}
