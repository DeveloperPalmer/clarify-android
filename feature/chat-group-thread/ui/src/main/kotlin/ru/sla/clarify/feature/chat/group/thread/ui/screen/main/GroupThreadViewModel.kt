package ru.sla.clarify.feature.chat.group.thread.ui.screen.main

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadModel
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Group
import ru.sla.clarify.feature.chat.group.thread.ui.routing.FlowEvent
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class GroupThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val groupThreadModel: GroupThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to {
      groupThreadModel.fetchHistoryCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GroupThreadDismissed)
      }
    }

    onEach(intent(ViewIntents::openGroupInfo)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GroupInfoRequested)
      }
    }

    onEach(groupThreadModel.group.filterNotNull()) {
      transitionTo { state, group ->
        state.copy(
          group = Group(
            id = group.id.value,
            name = group.name,
            memberCount = group.memberCount
          )
        )
      }
    }

    onEach(
      groupThreadModel.fetchHistoryCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(
      combine(
        groupThreadModel.commits,
        groupThreadModel.members
      ) { commits, members ->
        commits.toUiCommits(memberNames = members.associate { it.id to it.displayName })
      }
    ) {
      transitionTo { state, items ->
        state.copy(commits = items)
      }
    }

    onEach(groupThreadModel.unreadCount) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        groupThreadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(intent(ViewIntents::sendMessage)) {
      action { _, _, text ->
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
          groupThreadModel.sendMessage.start(trimmed)
        }
      }
    }

    onEach(groupThreadModel.sendMessage.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(R.string.group_thread_send_error)
          )
        )
      }
    }
  }
}
