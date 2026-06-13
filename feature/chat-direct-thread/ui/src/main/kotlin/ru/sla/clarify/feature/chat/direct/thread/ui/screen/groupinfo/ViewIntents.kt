package ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupinfo

import ru.sla.clarify.feature.chat.direct.thread.ui.entity.GroupMember
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.InviteCandidate
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val showRenameDialog = intent(name = "showRenameDialog")
  val confirmRename = intent<String>(name = "confirmRename")
  val showInviteSheet = intent(name = "showInviteSheet")
  val changeSearchQuery = intent<String>(name = "changeSearchQuery")
  val toggleCandidate = intent<InviteCandidate>(name = "toggleCandidate")
  val confirmInvite = intent(name = "confirmInvite")
  val requestRemoveMember = intent<GroupMember>(name = "requestRemoveMember")
  val confirmRemoveMember = intent<String>(name = "confirmRemoveMember")
  val requestLeaveGroup = intent(name = "requestLeaveGroup")
  val confirmLeaveGroup = intent(name = "confirmLeaveGroup")
  val requestDeleteGroup = intent(name = "requestDeleteGroup")
  val confirmDeleteGroup = intent(name = "confirmDeleteGroup")
}
