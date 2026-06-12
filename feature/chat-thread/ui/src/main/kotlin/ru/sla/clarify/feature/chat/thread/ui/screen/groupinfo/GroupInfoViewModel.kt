package ru.sla.clarify.feature.chat.thread.ui.screen.groupinfo

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.chat.conversation.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.thread.domain.GroupThreadModel
import ru.sla.clarify.feature.chat.thread.ui.entity.Group
import ru.sla.clarify.feature.chat.thread.ui.entity.GroupMember
import ru.sla.clarify.feature.chat.thread.ui.entity.InviteCandidate
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class GroupInfoViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val groupThreadModel: GroupThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GroupInfoDismissed)
      }
    }

    onEach(
      combine(
        groupThreadModel.group.filterNotNull(),
        groupThreadModel.members,
        groupThreadModel.user.filterNotNull()
      ) { group, members, user ->
        Triple(group, members, user)
      }
    ) {
      transitionTo { state, (group, members, user) ->
        state.copy(
          group = Group(
            id = group.id.value,
            name = group.name,
            memberCount = group.memberCount
          ),
          isOwner = group.ownerId == user.id,
          members = members.map { member ->
            GroupMember(
              id = member.id.value,
              displayName = member.displayName.orEmpty().ifBlank { "?" },
              email = member.email.orEmpty(),
              photoUrl = member.photoUrl,
              isOwner = member.isOwner,
              isMe = member.isMe
            )
          }
        )
      }
    }

    configureRenameTransitions()
    configureInviteTransitions()
    configureDestructiveTransitions()
  }

  private fun MachineDsl<ViewState>.configureRenameTransitions() {
    onEach(intent(ViewIntents::showRenameDialog)) {
      action { state, _, _ ->
        sendViewEvent(showRenameGroupDialog(currentName = state.group?.name.orEmpty()))
      }
    }

    onEach(intent(ViewIntents::confirmRename)) {
      action { _, _, name ->
        groupThreadModel.renameGroup.start(name)
      }
    }

    onEach(groupThreadModel.renameGroup.jobFlow.errors()) {
      action { _, _, _ -> sendActionFailedSnackbar() }
    }
  }

  private fun MachineDsl<ViewState>.configureInviteTransitions() {
    onEach(intent(ViewIntents::showInviteSheet)) {
      transitionTo { state, _ ->
        state.copy(
          searchQuery = "",
          searchResults = emptyList(),
          selectedCandidates = emptyList()
        )
      }
      action { _, _, _ ->
        sendViewEvent(showInviteMembersSheet())
      }
    }

    onEach(intent(ViewIntents::changeSearchQuery)) {
      transitionTo { state, query ->
        state.copy(
          searchQuery = query,
          searchResults = if (query.isBlank()) emptyList() else state.searchResults
        )
      }
    }

    onEach(
      intent(ViewIntents::changeSearchQuery)
        .map { it.trim() }
        .debounce(SEARCH_DEBOUNCE_MS)
        .filter { it.isNotEmpty() }
    ) {
      action { _, _, query ->
        groupThreadModel.searchUsers.start(query)
      }
    }

    onEach(groupThreadModel.searchUsers.jobFlow.successResults()) {
      transitionTo { state, foundUsers ->
        state.copy(searchResults = foundUsers.toCandidates(state))
      }
    }

    onEach(intent(ViewIntents::toggleCandidate)) {
      transitionTo { state, candidate ->
        val isSelected = state.selectedCandidates.any { it.id == candidate.id }
        val updatedSelected = if (isSelected) {
          state.selectedCandidates.filter { it.id != candidate.id }
        } else if (candidate.isAlreadyMember || state.isInviteLimitReached()) {
          state.selectedCandidates
        } else {
          state.selectedCandidates + candidate.copy(isSelected = true)
        }
        state
          .copy(selectedCandidates = updatedSelected)
          .refreshCandidateFlags()
      }
    }

    onEach(intent(ViewIntents::confirmInvite)) {
      action { state, _, _ ->
        val userIds = state.selectedCandidates.map { UserId(it.id) }
        if (userIds.isNotEmpty()) {
          groupThreadModel.inviteMembers.start(userIds)
        }
      }
    }

    onEach(groupThreadModel.inviteMembers.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(
          searchQuery = "",
          searchResults = emptyList(),
          selectedCandidates = emptyList()
        )
      }
    }

    onEach(groupThreadModel.inviteMembers.jobFlow.errors()) {
      action { _, _, _ -> sendActionFailedSnackbar() }
    }
  }

  private fun MachineDsl<ViewState>.configureDestructiveTransitions() {
    onEach(intent(ViewIntents::requestRemoveMember)) {
      action { _, _, member ->
        sendViewEvent(showRemoveMemberDialog(member))
      }
    }

    onEach(intent(ViewIntents::confirmRemoveMember)) {
      action { _, _, memberId ->
        groupThreadModel.removeMember.start(UserId(memberId))
      }
    }

    onEach(intent(ViewIntents::requestLeaveGroup)) {
      action { _, _, _ ->
        sendViewEvent(showLeaveGroupDialog())
      }
    }

    onEach(intent(ViewIntents::confirmLeaveGroup)) {
      action { _, _, _ ->
        groupThreadModel.leaveGroup.start()
      }
    }

    onEach(intent(ViewIntents::requestDeleteGroup)) {
      action { _, _, _ ->
        sendViewEvent(showDeleteGroupDialog())
      }
    }

    onEach(intent(ViewIntents::confirmDeleteGroup)) {
      action { _, _, _ ->
        groupThreadModel.deleteGroup.start()
      }
    }

    onEach(groupThreadModel.leaveGroup.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GroupClosed)
      }
    }

    onEach(groupThreadModel.deleteGroup.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GroupClosed)
      }
    }

    onEach(groupThreadModel.removeMember.jobFlow.errors()) {
      action { _, _, _ -> sendActionFailedSnackbar() }
    }

    onEach(groupThreadModel.leaveGroup.jobFlow.errors()) {
      action { _, _, _ -> sendActionFailedSnackbar() }
    }

    onEach(groupThreadModel.deleteGroup.jobFlow.errors()) {
      action { _, _, _ -> sendActionFailedSnackbar() }
    }
  }

  private fun sendActionFailedSnackbar() {
    sendViewEvent(
      Snackbar(
        isError = true,
        message = resRef(R.string.group_info_action_failed)
      )
    )
  }
}

private fun List<FoundUser>.toCandidates(state: ViewState): List<InviteCandidate> {
  val memberIds = state.members.mapTo(mutableSetOf()) { it.id }
  val selectedIds = state.selectedCandidates.mapTo(mutableSetOf()) { it.id }
  return this
    .filter { it.id.value !in selectedIds }
    .map { user ->
      InviteCandidate(
        id = user.id.value,
        displayName = user.displayName,
        email = user.email,
        photoUrl = user.photoUrl,
        isAlreadyMember = user.id.value in memberIds,
        isSelected = false
      )
    }
}

private fun ViewState.refreshCandidateFlags(): ViewState {
  val selectedIds = selectedCandidates.mapTo(mutableSetOf()) { it.id }
  return copy(
    searchResults = searchResults.filter { it.id !in selectedIds },
    isInviteLimitReached = isInviteLimitReached()
  )
}

private fun ViewState.isInviteLimitReached(): Boolean {
  return members.size + selectedCandidates.size >= GROUP_MEMBER_LIMIT
}

private const val GROUP_MEMBER_LIMIT = 10
private const val SEARCH_DEBOUNCE_MS = 300L
