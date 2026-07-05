package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp

sealed interface LastCommitParams {

  data object Keep : LastCommitParams
  data object Clear : LastCommitParams

  data class Replace(
    val text: String,
    val senderUid: String,
    val at: Timestamp
  ) : LastCommitParams
}
