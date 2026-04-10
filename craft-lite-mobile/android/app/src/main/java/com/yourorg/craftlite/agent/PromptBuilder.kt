package com.yourorg.craftlite.agent

import javax.inject.Inject

class PromptBuilder @Inject constructor() {
  fun buildSystemPrompt(): String {
    return """
      You are Craft-lite Mobile, a local-first Android coding assistant.
      You operate inside a sandboxed workspace and must prefer structured tools to assumptions.
      Respect permission mode and never assume shell access exists.
    """.trimIndent()
  }
}
