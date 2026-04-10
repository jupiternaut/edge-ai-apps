package com.yourorg.craftlite.domain.tools

data class ToolRequest(
  val toolName: String,
  val argumentsJson: String,
)

data class ToolResult(
  val toolName: String,
  val outputText: String,
  val isError: Boolean = false,
)

data class PatchPreview(
  val originalText: String,
  val updatedText: String,
)
