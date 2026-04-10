package com.yourorg.craftlite.data.repo

import com.yourorg.craftlite.domain.session.ChatMessage
import com.yourorg.craftlite.domain.session.SessionSummary

interface SessionRepository {
  suspend fun listSessions(): List<SessionSummary>
  suspend fun createSession(workspaceId: String, title: String): SessionSummary
  suspend fun listMessages(sessionId: String): List<ChatMessage>
  suspend fun appendMessage(message: ChatMessage)
}
