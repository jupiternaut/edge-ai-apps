package com.yourorg.craftlite.agent

import com.yourorg.craftlite.skills.SkillPromptFormatter
import javax.inject.Inject

class PromptBuilder @Inject constructor(
  private val skillPromptFormatter: SkillPromptFormatter,
) {
  fun buildSystemPrompt(context: PromptContext): String {
    val workspaceSection =
      context.workspace?.let {
        """
        |== ACTIVE WORKSPACE ==
        |name: ${it.name}
        |root: ${it.rootPath}
        """.trimMargin()
      } ?: "== ACTIVE WORKSPACE ==\nnone"

    val permissionSection =
      """
      |== PERMISSION MODE ==
      |${context.permissionMode.name}
      |
      |Rules:
      |- SAFE: read-only
      |- ASK: request approval before writes
      |- AUTO: only sandboxed writes may proceed automatically
      """.trimMargin()

    val messagesSection =
      if (context.recentMessages.isEmpty()) {
        "== RECENT SESSION CONTEXT ==\nnone"
      } else {
        buildString {
          appendLine("== RECENT SESSION CONTEXT ==")
          context.recentMessages.takeLast(6).forEach { message ->
            appendLine("${message.role.name.lowercase()}: ${message.content.take(240)}")
          }
        }.trim()
      }

    val skillsSection =
      skillPromptFormatter.format(context.activeSkills).takeIf { it.isNotBlank() }?.let {
        "== ACTIVE SKILLS ==\n$it"
      } ?: "== ACTIVE SKILLS ==\nnone"

    val snippetSection =
      if (context.relevantFileSnippets.isEmpty()) {
        "== RELEVANT FILE SNIPPETS ==\nnone"
      } else {
        buildString {
          appendLine("== RELEVANT FILE SNIPPETS ==")
          context.relevantFileSnippets.forEachIndexed { index, snippet ->
            appendLine("[snippet ${index + 1}]")
            appendLine(snippet.take(500))
          }
        }.trim()
      }

    return """
      You are Craft-lite Mobile, a local-first Android coding assistant.
      You operate inside a sandboxed workspace and must prefer structured tools to assumptions.
      Respect permission mode and never assume shell access exists.

      $workspaceSection

      $permissionSection

      $messagesSection

      $skillsSection

      $snippetSection
    """.trimIndent().trim()
  }
}
