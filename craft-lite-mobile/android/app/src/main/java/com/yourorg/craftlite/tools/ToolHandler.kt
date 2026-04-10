package com.yourorg.craftlite.tools

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult

interface ToolHandler {
  val name: String
  suspend fun execute(request: ToolRequest): ToolResult
}
