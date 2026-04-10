package com.yourorg.craftlite.agent

import com.yourorg.craftlite.domain.tools.ToolResult

sealed class AgentEvent {
  data class Status(val value: String) : AgentEvent()
  data class TextDelta(val value: String) : AgentEvent()
  data class TextComplete(val value: String) : AgentEvent()
  data class ToolStarted(val toolName: String) : AgentEvent()
  data class ToolFinished(val result: ToolResult) : AgentEvent()
  data class Error(val message: String) : AgentEvent()
  data object Complete : AgentEvent()
}

data class ConversationTurnRequest(
  val sessionId: String,
  val workspaceId: String,
  val userMessage: String,
)
