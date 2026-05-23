package ru.sla.clarify.database

import ru.sla.log.log

// TODO @sla @DB @Cleanup Improve DB cleanup
//   Several problems as of now:
//   1. Everything is deleted globally, for all session keys. Correct approach would be for all tables to have
//        a 'sessionKey' column and delete only for this key
//   2. Developer has to remember to add stuff here whenever a new feature is added.
//      This approach may be inverted, so that each
//      feature has some kind of "registerSessionKeyCleanupCallback" which would be called by some kind of a cleanup
//      director when the time comes to delete everything. Having it centralized here may be not very good

fun InMemoryDB.cleanupBySessionKey(key: String) {
  transaction {
    log { "cleaning up data for session key=$key" }
    chatConversationQueries.deleteAll()
    chatCommitQueries.deleteAll()
    userDetailsQueries.deleteAll()
  }
}
