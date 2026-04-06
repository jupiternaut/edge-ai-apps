package com.google.ai.edge.gallery.customtasks.edgecodex

/**
 * EdgeCodex task definition.
 * Configures Gemma 4 E2B as a code-specialized assistant.
 *
 * Unlike TextPlay (which uses FunctionGemma for structured function calls),
 * EdgeCodex uses Gemma 4 E2B in conversational mode for free-form code
 * analysis, generation, and refactoring.
 */
object EdgeCodexTask {

    const val TASK_TYPE = "llm_edgecodex"

    /**
     * System prompt that configures Gemma 4 E2B as a coding expert.
     * Includes the user's current code context for relevant responses.
     */
    fun buildSystemPrompt(
        language: String = "python",
        code: String? = null,
        customPrompt: String? = null,
    ): String {
        val codeContext = code?.let {
            """
            |
            |== CURRENT CODE (${language}) ==
            |```${language}
            |${it.take(4000)}
            |```
            """.trimMargin()
        } ?: ""

        val base = customPrompt ?: DEFAULT_SYSTEM_PROMPT

        return """
        |$base
        |
        |== ACTIVE LANGUAGE ==
        |$language
        |
        |== RESPONSE FORMAT ==
        |1. Be concise. Lead with the answer, not the reasoning.
        |2. Always use markdown code blocks with language tags.
        |3. When suggesting fixes, show the corrected code, not just the explanation.
        |4. For refactoring, show before/after comparison.
        |5. End responses with a brief one-line summary.
        |$codeContext
        """.trimMargin()
    }

    private const val DEFAULT_SYSTEM_PROMPT = """You are EdgeCodex, an expert coding assistant running entirely on-device via Gemma 4 E2B.
Your key strengths:
- Code explanation and documentation
- Bug detection and fixing
- Refactoring and optimization suggestions
- Code completion and generation
- Unit test generation
- Performance analysis

You operate 100% offline. The user's code never leaves their device.
Always provide practical, actionable advice with code examples."""
}
