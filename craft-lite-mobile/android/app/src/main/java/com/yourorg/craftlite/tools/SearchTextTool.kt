package com.yourorg.craftlite.tools

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import javax.inject.Inject

class SearchTextTool @Inject constructor() : ToolHandler {
  override val name: String = "search_text"

  override suspend fun execute(request: ToolRequest): ToolResult {
    return ToolResult(
      toolName = name,
      outputText = "Phase 1 placeholder: search index not wired yet.",
    )
  }
}
