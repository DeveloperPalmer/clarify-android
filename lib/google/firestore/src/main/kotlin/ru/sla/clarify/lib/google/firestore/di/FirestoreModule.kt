package ru.sla.clarify.lib.google.firestore.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.chat.api.BranchApi
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.ConversationApi
import ru.sla.clarify.chat.api.MemberApi
import ru.sla.clarify.chat.api.MergeRequestApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.lib.google.firestore.Firestore

@Module
@ContributesTo(AppScope::class)
interface FirestoreModule {
  @Binds
  fun bindBranchApi(impl: Firestore): BranchApi

  @Binds
  fun bindCommitApi(impl: Firestore): CommitApi

  @Binds
  fun bindConversationApi(impl: Firestore): ConversationApi

  @Binds
  fun bindMergeRequestApi(impl: Firestore): MergeRequestApi

  @Binds
  fun bindMemberApi(impl: Firestore): MemberApi

  @Binds
  fun bindUnreadCountApi(impl: Firestore): UnreadCountApi

  @Binds
  fun bindUserApi(impl: Firestore): UserApi
}
