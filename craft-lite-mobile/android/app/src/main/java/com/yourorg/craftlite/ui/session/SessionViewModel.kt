package com.yourorg.craftlite.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourorg.craftlite.agent.ConversationTurnRequest
import com.yourorg.craftlite.agent.EventReducer
import com.yourorg.craftlite.agent.MobileAgentBackend
import com.yourorg.craftlite.agent.StreamingState
import com.yourorg.craftlite.data.repo.SessionRepository
import com.yourorg.craftlite.domain.session.ChatMessage
import com.yourorg.craftlite.domain.session.MessageRole
import com.yourorg.craftlite.domain.session.TurnState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
  private val agentBackend: MobileAgentBackend,
  private val eventReducer: EventReducer,
  private val sessionRepository: SessionRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(SessionUiState())
  val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
  private var activeTurnJob: Job? = null

  init {
    loadHistory()
  }

  fun setDraftMessage(value: String) {
    _uiState.update { it.copy(draftMessage = value) }
  }

  fun cancelTurn() {
    activeTurnJob?.cancel()
    activeTurnJob = null
    _uiState.update {
      it.copy(
        streamingState = it.streamingState.copy(
          status = "Cancelled by user",
          turnState = TurnState.CANCELLED,
        )
      )
    }
  }

  fun sendMessage() {
    if (activeTurnJob != null) return

    val draft = _uiState.value.draftMessage.trim()
    if (draft.isBlank()) return

    activeTurnJob = viewModelScope.launch {
      val sessionId = "phase2-session"
      val userMessage = ChatMessage(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        role = MessageRole.USER,
        content = draft,
        createdAtMillis = System.currentTimeMillis(),
      )
      sessionRepository.appendMessage(userMessage)

      _uiState.update {
        it.copy(
          draftMessage = "",
          lastUserMessage = draft,
          messages = it.messages + userMessage,
          streamingState = StreamingState(
            status = "Starting",
            turnState = TurnState.PREPARING,
          ),
        )
      }

      try {
        agentBackend.sendMessage(
          ConversationTurnRequest(
            sessionId = sessionId,
            workspaceId = "sample-workspace",
            userMessage = draft,
          )
        ).collect { event ->
          _uiState.update { current ->
            current.copy(streamingState = eventReducer.reduce(current.streamingState, event))
          }
        }

        val finalState = _uiState.value.streamingState
        if (finalState.turnState == TurnState.COMPLETE && finalState.text.isNotBlank()) {
          val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = finalState.text,
            createdAtMillis = System.currentTimeMillis(),
          )
          sessionRepository.appendMessage(assistantMessage)
          _uiState.update { current ->
            current.copy(messages = current.messages + assistantMessage)
          }
        }
      } finally {
        activeTurnJob = null
      }
    }
  }

  private fun loadHistory() {
    viewModelScope.launch {
      val messages = sessionRepository.listMessages("phase2-session")
      _uiState.update { current ->
        current.copy(messages = messages)
      }
    }
  }
}

data class SessionUiState(
  val draftMessage: String = "",
  val lastUserMessage: String? = null,
  val messages: List<ChatMessage> = emptyList(),
  val streamingState: StreamingState = StreamingState(),
)
