package com.yourorg.craftlite.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelLifecycleManager @Inject constructor(
  private val llmEngine: LlmEngine,
) {
  suspend fun prepare() {
    llmEngine.warmup()
  }

  suspend fun release() {
    llmEngine.shutdown()
  }
}
