package ru.sla.clarify.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.SettingsDatabase
import ru.sla.clarify.database.User
import ru.sla.clarify.database.adapter.BranchIdAdapter
import ru.sla.clarify.database.adapter.CommitIdAdapter
import ru.sla.clarify.database.adapter.CommitReplyAdapter
import ru.sla.clarify.database.adapter.ConversationIdAdapter
import ru.sla.clarify.database.adapter.MemberIdAdapter
import ru.sla.clarify.database.adapter.UserIdAdapter
import ru.sla.clarify.database.adapter.UserIdSetAdapter
import ru.sla.clarify.database.chat.ChatBranch
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.chat.ChatConversation
import ru.sla.clarify.database.chat.ChatMember
import ru.sla.clarify.database.chat.MergeRequest
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@ContributesTo(AppScope::class)
interface DatabaseModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettingsDatabase(@ApplicationContext context: Context): SettingsDatabase {
    return Room.databaseBuilder(context, SettingsDatabase::class.java, "settings.db")
      .setDriver(AndroidSQLiteDriver())
      .build()
  }

  @SingleIn(AppScope::class)
  @Provides
  fun provideInMemoryDatabase(@ApplicationContext context: Context): InMemoryDB {
    val driver = AndroidSqliteDriver(InMemoryDB.Schema, context, name = null)
    return InMemoryDB(
      driver = driver,
      ChatConversationAdapter = ChatConversation.Adapter(
        idAdapter = ConversationIdAdapter,
        ownerIdAdapter = UserIdAdapter,
        lastCommitSenderIdAdapter = UserIdAdapter
      ),
      UserAdapter = User.Adapter(
        idAdapter = UserIdAdapter
      ),
      ChatBranchAdapter = ChatBranch.Adapter(
        idAdapter = BranchIdAdapter,
        conversationIdAdapter = ConversationIdAdapter,
        parentBranchIdAdapter = BranchIdAdapter,
        branchedFromCommitIdAdapter = CommitIdAdapter,
        createdByIdAdapter = UserIdAdapter
      ),
      ChatCommitAdapter = ChatCommit.Adapter(
        idAdapter = CommitIdAdapter,
        conversationIdAdapter = ConversationIdAdapter,
        branchIdAdapter = BranchIdAdapter,
        senderIdAdapter = UserIdAdapter,
        replyCommitAdapter = CommitReplyAdapter,
        invitedIdAdapter = UserIdAdapter
      ),
      ChatMemberAdapter = ChatMember.Adapter(
        idAdapter = MemberIdAdapter,
        conversationIdAdapter = ConversationIdAdapter
      ),
      MergeRequestAdapter = MergeRequest.Adapter(
        branchIdAdapter = BranchIdAdapter,
        initiatorIdAdapter = UserIdAdapter,
        approvedByIdsAdapter = UserIdSetAdapter,
        mergedIntoBranchIdAdapter = BranchIdAdapter
      )
    )
  }

  @SingleIn(AppScope::class)
  @Provides
  fun providePersistedDatabase(@ApplicationContext context: Context): PersistedDB {
    val driver = AndroidSqliteDriver(PersistedDB.Schema, context, name = "clarify.db")
    return PersistedDB(driver)
  }
}
