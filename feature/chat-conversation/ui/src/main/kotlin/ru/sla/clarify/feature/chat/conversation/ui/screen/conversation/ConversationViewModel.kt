package ru.sla.clarify.feature.chat.conversation.ui.screen.conversation

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.plexus.core.FeatureConfigsManager
import ru.kode.remo.QueueingStrategy
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.core.domain.toggle.isFeatureEnabledLive
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.ConversationModel
import ru.sla.clarify.feature.chat.conversation.domain.entity.PeerNotFoundException
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent
import ru.sla.clarify.feature.chat.conversation.ui.screen.conversation.ViewState.CreateConversationTab
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef

class ConversationViewModel(
  dispatcher: CoroutineDispatcher,
  private val eventSink: FlowEventSink,
  private val conversationModel: ConversationModel,
  private val featureConfigsManager: FeatureConfigsManager
) : ViewModel<ViewState, ViewIntents>(dispatcher) {

  @Inject
  constructor(
    eventSink: FlowEventSink,
    conversationModel: ConversationModel,
    featureConfigsManager: FeatureConfigsManager
  ) : this(Dispatchers.Default, eventSink, conversationModel, featureConfigsManager)

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      transitionTo { state, _ ->
        state.copy(
          editModeEnabled = false,
          selectedConversationsIds = emptyList()
        )
      }
      action { state, _, _ ->
        if (!state.editModeEnabled) {
          eventSink.sendEvent(FlowEvent.ConversationDismissed)
        }
      }
    }

    onEach(intent(ViewIntents::openProfile)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ProfileRequested)
      }
    }

    onEach(intent(ViewIntents::openCreateConversation)) {
      action { _, _, _ ->
        sendViewEvent(showCreateConversationDialog())
      }
    }

    onEach(intent(ViewIntents::hideCreateConversation)) {
      transitionTo { state, _ ->
        state.copy(
          createConversationLoadState = ContentLoadState.NotStarted,
          groupNameQuery = TextFieldValue(),
          groupNameError = null,
          directEmailQuery = TextFieldValue(),
          directEmailError = null
        )
      }
    }

    onEach(intent(ViewIntents::changeCreateConversationTab)) {
      transitionTo { state, tab ->
        state.copy(
          selectedCreateConversationTab = tab as CreateConversationTab,
          directEmailError = null,
          groupNameError = null
        )
      }
    }

    onEach(conversationModel.user) {
      transitionTo { state, user ->
        state.copy(user = user)
      }
    }

    onEach(featureConfigsManager.isFeatureEnabledLive(AppFeature.GroupsAvailable)) {
      transitionTo { state, groupsAvailable ->
        state.copy(groupsAvailable = groupsAvailable)
      }
    }

    onEach(
      combine(
        conversationModel.conversations,
        featureConfigsManager.isFeatureEnabledLive(AppFeature.GroupsAvailable)
      ) { conversations, groupsAvailable ->
        if (groupsAvailable) {
          conversations
        } else {
          conversations.filterNot { it is Conversation.Group }
        }
      }
    ) {
      transitionTo { state, conversations ->
        state.copy(conversations = conversations)
      }
    }

    configureDirectConversationTransitions()
    configureGroupConversationTransitions()
    configureDeleteConversationTransitions()
  }

  private fun MachineDsl<ViewState>.configureDirectConversationTransitions() {
    onEach(intent(ViewIntents::openDirectConversation)) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId))
      }
    }

    onEach(intent(ViewIntents::showCreateDirectConversationError)) {
      transitionTo { state, error ->
        state.copy(directEmailError = error)
      }
    }

    onEach(intent(ViewIntents::hideCreateDirectConversationError)) {
      transitionTo { state, _ ->
        state.copy(directEmailError = null)
      }
    }

    onEach(intent(ViewIntents::changeEmailQuery)) {
      transitionTo { state, directEmailQuery ->
        state.copy(
          directEmailQuery = directEmailQuery,
          directEmailError = null
        )
      }
    }

    onEach(intent(ViewIntents::validateDirectEmail)) {
      transitionTo { state, _ ->
        val directEmailError = Email.validate(
          peerEmail = state.directEmailQuery.text,
          userEmail = state.user?.email
        ).fold(
          ifRight = { null },
          ifLeft = { it.first() }
        )
        state.copy(directEmailError = directEmailError)
      }
      action { _, newState, _ ->
        Email.validate(
          peerEmail = newState.directEmailQuery.text,
          userEmail = newState.user?.email
        ).onRight { email ->
          conversationModel.getPeerByEmail.start(
            argument = email,
            queueingStrategy = QueueingStrategy.SkipNew
          )
        }
      }
    }

    onEach(
      conversationModel.getPeerByEmail.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(createConversationLoadState = contentLoadState)
      }
    }

    onEach(
      conversationModel.getPeerByEmail.jobFlow
        .successResults()
        .zip(intent(ViewIntents::confirmCreateDirectConversation), ::Pair)
    ) {
      action { _, _, (peerId, _) ->
        eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId))
      }
    }

    onEach(conversationModel.getPeerByEmail.jobFlow.errors()) {
      action { _, _, error ->
        val message = when (error) {
          is PeerNotFoundException -> resRef(R.string.direct_conversation_error_user_not_found)
          else -> resRef(R.string.direct_conversation_error_lookup_failed)
        }
        sendViewEvent(Snackbar(isError = true, message = message))
      }
    }
  }

  private fun MachineDsl<ViewState>.configureGroupConversationTransitions() {
    onEach(intent(ViewIntents::openGroupConversation)) {
      action { state, _, conversationId ->
        if (state.groupsAvailable) {
          eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId))
        }
      }
    }

    onEach(intent(ViewIntents::validateGroupName)) {
      transitionTo { state, _ ->
        val groupNameError = GroupName.validate(
          name = state.groupNameQuery.text
        ).fold(
          ifRight = { null },
          ifLeft = { it.first() }
        )
        state.copy(groupNameError = groupNameError)
      }
      action { _, newState, _ ->
        GroupName.validate(
          name = newState.groupNameQuery.text
        ).onRight { groupName ->
          conversationModel.createGroup.start(
            argument = groupName,
            queueingStrategy = QueueingStrategy.SkipNew
          )
        }
      }
    }

    onEach(intent(ViewIntents::changeGroupNameQuery)) {
      transitionTo { state, groupNameQuery ->
        state.copy(
          groupNameQuery = groupNameQuery,
          groupNameError = null
        )
      }
    }

    onEach(
      conversationModel.createGroup.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(createConversationLoadState = contentLoadState)
      }
    }

    onEach(
      conversationModel.createGroup.jobFlow
        .successResults()
        .zip(intent(ViewIntents::confirmCreateGroup), ::Pair)
    ) {
      transitionTo { state, _ ->
        state.copy(createConversationLoadState = ContentLoadState.NotStarted)
      }
      action { _, _, (conversationId, _) ->
        eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId))
      }
    }

    onEach(conversationModel.createGroup.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(R.string.group_conversation_error_create_failed)
          )
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureDeleteConversationTransitions() {
    onEach(intent(ViewIntents::handleConversationLongPress)) {
      transitionTo { state, conversationId ->
        val updated = if (state.selectedConversationsIds.contains(conversationId)) {
          state.selectedConversationsIds.minus(conversationId)
        } else {
          state.selectedConversationsIds.plus(conversationId)
        }
        state.copy(
          editModeEnabled = !state.editModeEnabled || updated.any { it != conversationId },
          selectedConversationsIds = updated
        )
      }
    }

    onEach(intent(ViewIntents::openDeleteConversation)) {
      action { _, _, _ ->
        sendViewEvent(showDeleteConversationDialog())
      }
    }

    onEach(intent(ViewIntents::confirmDeleteConversation)) {
      action { state, _, _ ->
        conversationModel.deleteConversations.start(state.selectedConversationsIds)
      }
    }

    onEach(conversationModel.deleteConversations.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(
          editModeEnabled = false,
          selectedConversationsIds = emptyList()
        )
      }
    }
  }
}
