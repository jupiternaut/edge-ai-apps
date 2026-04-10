package com.yourorg.craftlite.tools

import com.yourorg.craftlite.data.repo.WorkspaceRepository
import com.yourorg.craftlite.domain.tools.ToolRequest
import com.yourorg.craftlite.domain.tools.ToolResult
import javax.inject.Inject

class ListFilesTool @Inject constructor(
  private val workspaceRepository: WorkspaceRepository,
) : ToolHandler {
  override val name: String = "list_files"

  override suspend fun execute(request: ToolRequest): ToolResult {
    val files = workspaceRepository.listFiles("sample-workspace")
    return ToolResult(
      toolName = name,
      outputText = files.joinToString("\n") { if (it.isDirectory) "[dir] ${it.name}" else it.name },
    )
  }
}
