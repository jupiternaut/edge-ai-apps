package com.yourorg.craftlite.agent

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.llm.LlmEngine
import com.yourorg.craftlite.llm.LlmOutput
import com.yourorg.craftlite.llm.LlmPrompt
import com.yourorg.craftlite.llm.ToolCallParser
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConversationCoordinator @Inject constructor(
  private val promptBuilder: PromptBuilder,
  private val llmEngine: LlmEngine,
  private val toolCallParser: ToolCallParser,
  private val toolRouter: ToolRouter,
) {
  fun runTurn(request: ConversationTurnRequest): Flow<AgentEvent> = flow {
    emit(AgentEvent.Status("Preparing prompt"))
    val prompt = LlmPrompt(
      systemPrompt = promptBuilder.buildSystemPrompt(),
      userMessage = request.userMessage,
    )

    llmEngine.stream(prompt).collect { output ->
      when (output) {
        is LlmOutput.Token -> emit(AgentEvent.TextDelta(output.text))
        is LlmOutput.FinalText -> emit(AgentEvent.TextComplete(output.text))
        is LlmOutput.ToolCall -> {
          val toolRequest = toolCallParser.parse(output.argumentsJson)
            ?: ToolRequest(output.name, output.argumentsJson)
          emit(AgentEvent.ToolStarted(toolRequest.toolName))
          val result = toolRouter.execute(toolRequest.copy(toolName = output.name))
          emit(AgentEvent.ToolFinished(result))
        }
      }
    }

    emit(AgentEvent.Complete)
  }
}
