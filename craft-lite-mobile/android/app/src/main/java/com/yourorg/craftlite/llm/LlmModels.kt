package com.yourorg.craftlite.llm

data class LlmPrompt(
  val systemPrompt: String,
  val userMessage: String,
)

sealed class LlmOutput {
  data class Token(val text: String) : LlmOutput()
  data class ToolCall(val name: String, val argumentsJson: String) : LlmOutput()
  data class FinalText(val text: String) : LlmOutput()
}
