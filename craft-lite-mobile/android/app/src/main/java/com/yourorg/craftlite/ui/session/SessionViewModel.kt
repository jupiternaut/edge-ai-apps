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
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
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

  fun setDraftMessage(value: String) {
    _uiState.update { it.copy(draftMessage = value) }
  }

  fun sendMessage() {
    val draft = _uiState.value.draftMessage.trim()
    if (draft.isBlank()) return

    viewModelScope.launch {
      val sessionId = "phase1-session"
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
          streamingState = StreamingState(status = "Starting"),
        )
      }

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
    }
  }
}

data class SessionUiState(
  val draftMessage: String = "",
  val lastUserMessage: String? = null,
  val streamingState: StreamingState = StreamingState(),
)
