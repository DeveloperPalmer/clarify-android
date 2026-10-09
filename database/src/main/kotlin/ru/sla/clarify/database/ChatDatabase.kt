package ru.sla.clarify.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import ru.sla.clarify.database.converter.BranchIdConverter
import ru.sla.clarify.database.converter.CommitReplyConverter
import ru.sla.clarify.database.converter.DistinctFlowConverter
import ru.sla.clarify.database.converter.MemberIdConverter
import ru.sla.clarify.database.converter.UserIdSetConverter
import ru.sla.clarify.database.dao.ChatBranchDao
import ru.sla.clarify.database.dao.ChatCommitDao
import ru.sla.clarify.database.dao.ChatConversationDao
import ru.sla.clarify.database.dao.ChatMemberDao
import ru.sla.clarify.database.dao.UserDao
import ru.sla.clarify.database.entity.ChatBranchEntity
import ru.sla.clarify.database.entity.ChatCommitEntity
import ru.sla.clarify.database.entity.ChatConversationEntity
import ru.sla.clarify.database.entity.ChatMemberEntity
import ru.sla.clarify.database.entity.MergeRequestEntity
import ru.sla.clarify.database.entity.UserEntity

@Database(
  version = 1,
  entities = [
    ChatConversationEntity::class,
    UserEntity::class,
    ChatMemberEntity::class,
    ChatBranchEntity::class,
    ChatCommitEntity::class,
    MergeRequestEntity::class
  ],
  exportSchema = true
)
@ColumnTypeConverters(
  BranchIdConverter::class,
  MemberIdConverter::class,
  UserIdSetConverter::class,
  CommitReplyConverter::class
)
@DaoReturnTypeConverters(
  DistinctFlowConverter::class
)
abstract class ChatDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao

  abstract fun chatMemberDao(): ChatMemberDao

  abstract fun chatConversationDao(): ChatConversationDao

  abstract fun chatBranchDao(): ChatBranchDao

  abstract fun chatCommitDao(): ChatCommitDao
}
