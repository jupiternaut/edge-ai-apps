package com.yourorg.craftlite.tools

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import javax.inject.Inject

class WriteFileTool @Inject constructor() : ToolHandler {
  override val name: String = "write_file"

  override suspend fun execute(request: ToolRequest): ToolResult {
    return ToolResult(
      toolName = name,
      outputText = "Phase 1 placeholder: write path reserved for Phase 4.",
    )
  }
}
