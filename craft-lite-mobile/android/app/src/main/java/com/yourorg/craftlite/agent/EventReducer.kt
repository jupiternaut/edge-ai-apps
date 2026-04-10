package com.yourorg.craftlite.agent

import javax.inject.Inject

data class StreamingState(
  val text: String = "",
  val status: String? = null,
  val error: String? = null,
)

class EventReducer @Inject constructor() {
  fun reduce(state: StreamingState, event: AgentEvent): StreamingState {
    return when (event) {
      is AgentEvent.Status -> state.copy(status = event.value)
      is AgentEvent.TextDelta -> state.copy(text = state.text + event.value)
      is AgentEvent.TextComplete -> state.copy(text = event.value)
      is AgentEvent.Error -> state.copy(error = event.message)
      is AgentEvent.ToolStarted -> state.copy(status = "Running ${event.toolName}")
      is AgentEvent.ToolFinished -> state.copy(status = "Finished ${event.result.toolName}")
      AgentEvent.Complete -> state.copy(status = "Complete")
    }
  }
}
