package com.yourorg.craftlite.llm

import kotlinx.coroutines.flow.Flow

interface LlmEngine {
  suspend fun warmup()
  fun stream(prompt: LlmPrompt): Flow<LlmOutput>
  suspend fun shutdown()
}
