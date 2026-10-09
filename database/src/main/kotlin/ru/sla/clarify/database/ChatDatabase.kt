package ru.sla.clarify.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import ru.sla.clarify.database.converter.BranchIdConverter
import ru.sla.clarify.database.converter.CommitReplyConverter
import ru.sla.clarify.database.converter.MemberIdConverter
import ru.sla.clarify.database.converter.UserIdSetConverter
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
abstract class ChatDatabase : RoomDatabase()
