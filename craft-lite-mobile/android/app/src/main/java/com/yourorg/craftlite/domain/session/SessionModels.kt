package com.yourorg.craftlite.domain.session

data class SessionSummary(
  val id: String,
  val workspaceId: String,
  val title: String,
  val updatedAtMillis: Long,
)

enum class MessageRole {
  USER,
  ASSISTANT,
  SYSTEM,
  TOOL,
}

data class ChatMessage(
  val id: String,
  val sessionId: String,
  val role: MessageRole,
  val content: String,
  val createdAtMillis: Long,
)

enum class TurnState {
  IDLE,
  PREPARING,
  STREAMING,
  RUNNING_TOOL,
  AWAITING_PERMISSION,
  COMPLETE,
  FAILED,
  CANCELLED,
}
