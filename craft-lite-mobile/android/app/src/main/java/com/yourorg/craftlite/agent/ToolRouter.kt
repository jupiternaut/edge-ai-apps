package com.yourorg.craftlite.agent

import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import com.yourorg.craftlite.tools.ApplyPatchTool
import com.yourorg.craftlite.tools.ListFilesTool
import com.yourorg.craftlite.tools.ReadFileTool
import com.yourorg.craftlite.tools.SearchTextTool
import com.yourorg.craftlite.tools.ToolHandler
import com.yourorg.craftlite.tools.WriteFileTool
import javax.inject.Inject

class ToolRouter @Inject constructor(
  listFilesTool: ListFilesTool,
  readFileTool: ReadFileTool,
  searchTextTool: SearchTextTool,
  writeFileTool: WriteFileTool,
  applyPatchTool: ApplyPatchTool,
) {
  private val handlers: Map<String, ToolHandler> = listOf(
    listFilesTool,
    readFileTool,
    searchTextTool,
    writeFileTool,
    applyPatchTool,
  ).associateBy { it.name }

  suspend fun execute(request: ToolRequest): ToolResult {
    val handler = handlers[request.toolName]
      ?: return ToolResult(
        toolName = request.toolName,
        outputText = "Unknown tool: ${request.toolName}",
        isError = true,
      )
    return handler.execute(request)
  }
}
