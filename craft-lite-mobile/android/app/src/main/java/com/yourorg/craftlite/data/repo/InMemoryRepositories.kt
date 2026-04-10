package com.yourorg.craftlite.data.repo

import com.yourorg.craftlite.domain.permissions.PermissionMode
import com.yourorg.craftlite.domain.session.ChatMessage
import com.yourorg.craftlite.domain.session.SessionSummary
import com.yourorg.craftlite.domain.workspace.Workspace
import com.yourorg.craftlite.domain.workspace.WorkspaceFile
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemorySessionRepository @Inject constructor() : SessionRepository {
  private val sessions = mutableListOf<SessionSummary>()
  private val messages = mutableListOf<ChatMessage>()

  override suspend fun listSessions(): List<SessionSummary> = sessions.toList()

  override suspend fun createSession(workspaceId: String, title: String): SessionSummary {
    val session = SessionSummary(
      id = UUID.randomUUID().toString(),
      workspaceId = workspaceId,
      title = title,
      updatedAtMillis = System.currentTimeMillis(),
    )
    sessions.add(0, session)
    return session
  }

  override suspend fun listMessages(sessionId: String): List<ChatMessage> {
    return messages.filter { it.sessionId == sessionId }
  }

  override suspend fun appendMessage(message: ChatMessage) {
    messages.add(message)
  }
}

@Singleton
class InMemoryWorkspaceRepository @Inject constructor() : WorkspaceRepository {
  private val workspaces = mutableListOf(
    Workspace(
      id = "sample-workspace",
      name = "Sample Workspace",
      rootPath = "/data/data/com.yourorg.craftlite/files/workspaces/sample",
    )
  )

  override suspend fun listWorkspaces(): List<Workspace> = workspaces.toList()

  override suspend fun createWorkspace(name: String, rootPath: String): Workspace {
    val workspace = Workspace(
      id = UUID.randomUUID().toString(),
      name = name,
      rootPath = rootPath,
    )
    workspaces.add(workspace)
    return workspace
  }

  override suspend fun listFiles(workspaceId: String): List<WorkspaceFile> {
    return listOf(
      WorkspaceFile(path = "README.md", name = "README.md", isDirectory = false, sizeBytes = 1280),
      WorkspaceFile(path = "src", name = "src", isDirectory = true),
    )
  }

  override suspend fun readFile(path: String): String {
    return "// Placeholder file content for $path"
  }
}

@Singleton
class InMemorySettingsRepository @Inject constructor() : SettingsRepository {
  private var permissionMode: PermissionMode = PermissionMode.ASK

  override suspend fun getPermissionMode(): PermissionMode = permissionMode

  override suspend fun setPermissionMode(mode: PermissionMode) {
    permissionMode = mode
  }
}
