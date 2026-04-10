package com.yourorg.craftlite.llm

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class GemmaLiteRtEngine @Inject constructor() : LlmEngine {
  override suspend fun warmup() {
    // Phase 1 placeholder.
  }

  override fun stream(prompt: LlmPrompt): Flow<LlmOutput> = flow {
    emit(LlmOutput.Token("Craft-lite "))
    delay(30)
    emit(LlmOutput.Token("Mobile "))
    delay(30)
    emit(LlmOutput.FinalText("placeholder response for: ${prompt.userMessage.take(80)}"))
  }

  override suspend fun shutdown() {
    // Phase 1 placeholder.
  }
}
