package com.yourorg.craftlite.tools

import com.yourorg.craftlite.data.repo.WorkspaceRepository
import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import javax.inject.Inject

class ReadFileTool @Inject constructor(
  private val workspaceRepository: WorkspaceRepository,
) : ToolHandler {
  override val name: String = "read_file"

  override suspend fun execute(request: ToolRequest): ToolResult {
    val content = workspaceRepository.readFile("README.md")
    return ToolResult(toolName = name, outputText = content)
  }
}
