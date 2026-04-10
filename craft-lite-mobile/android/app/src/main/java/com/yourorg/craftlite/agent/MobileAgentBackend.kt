package com.yourorg.craftlite.agent

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class MobileAgentBackend @Inject constructor(
  private val coordinator: ConversationCoordinator,
) {
  fun sendMessage(request: ConversationTurnRequest): Flow<AgentEvent> {
    return coordinator.runTurn(request)
  }
}
