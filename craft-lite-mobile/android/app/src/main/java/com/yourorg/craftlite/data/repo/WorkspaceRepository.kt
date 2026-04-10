package com.yourorg.craftlite.data.repo

import com.yourorg.craftlite.domain.workspace.Workspace
import com.yourorg.craftlite.domain.workspace.WorkspaceFile

interface WorkspaceRepository {
  suspend fun listWorkspaces(): List<Workspace>
  suspend fun createWorkspace(name: String, rootPath: String): Workspace
  suspend fun listFiles(workspaceId: String): List<WorkspaceFile>
  suspend fun readFile(path: String): String
}
