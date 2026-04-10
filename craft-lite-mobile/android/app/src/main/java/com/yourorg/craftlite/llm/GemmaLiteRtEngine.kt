package com.yourorg.craftlite.llm

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@Singleton
class GemmaLiteRtEngine @Inject constructor() : LlmEngine {
  private var warmedUp: Boolean = false

  override suspend fun warmup() {
    delay(40)
    warmedUp = true
  }

  override fun stream(prompt: LlmPrompt): Flow<LlmOutput> = flow {
    if (!warmedUp) {
      warmup()
    }

    val text =
      buildString {
        append("Craft-lite Mobile local model placeholder. ")
        append("This response is streamed through the Phase 2 orchestration path. ")
        append("User request: ${prompt.userMessage.take(120)}. ")
        append("System context chars: ${prompt.systemPrompt.length}.")
      }

    val chunks = text.chunked(18)
    for (chunk in chunks) {
      if (!currentCoroutineContext().isActive) return@flow
      emit(LlmOutput.Token(chunk))
      delay(25)
    }

    emit(LlmOutput.FinalText(text))
  }

  override suspend fun shutdown() {
    warmedUp = false
  }
}
