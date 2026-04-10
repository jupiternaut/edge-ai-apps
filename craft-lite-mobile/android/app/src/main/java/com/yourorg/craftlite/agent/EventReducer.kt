package com.yourorg.craftlite.agent

import com.yourorg.craftlite.domain.session.TurnState
import javax.inject.Inject

data class StreamingState(
  val text: String = "",
  val status: String? = null,
  val error: String? = null,
  val turnState: TurnState = TurnState.IDLE,
)

class EventReducer @Inject constructor() {
  fun reduce(state: StreamingState, event: AgentEvent): StreamingState {
    return when (event) {
      is AgentEvent.Status -> state.copy(status = event.value)
      is AgentEvent.TurnStateChanged -> state.copy(turnState = event.state)
      is AgentEvent.TextDelta -> state.copy(
        text = state.text + event.value,
        turnState = TurnState.STREAMING,
        error = null,
      )
      is AgentEvent.TextComplete -> state.copy(
        text = event.value,
        turnState = TurnState.COMPLETE,
        error = null,
      )
      is AgentEvent.Error -> state.copy(
        error = event.message,
        turnState = TurnState.FAILED,
      )
      is AgentEvent.ToolStarted -> state.copy(
        status = "Running ${event.toolName}",
        turnState = TurnState.RUNNING_TOOL,
      )
      is AgentEvent.ToolFinished -> state.copy(
        status = "Finished ${event.result.toolName}",
        turnState = TurnState.STREAMING,
      )
      AgentEvent.Cancelled -> state.copy(
        status = "Cancelled",
        turnState = TurnState.CANCELLED,
      )
      AgentEvent.Complete -> state.copy(
        status = "Complete",
        turnState = TurnState.COMPLETE,
      )
    }
  }
}
