package com.yourorg.craftlite.tools

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import javax.inject.Inject

class ApplyPatchTool @Inject constructor() : ToolHandler {
  override val name: String = "apply_patch"

  override suspend fun execute(request: ToolRequest): ToolResult {
    return ToolResult(
      toolName = name,
      outputText = "Phase 1 placeholder: patch application reserved for Phase 4.",
    )
  }
}
