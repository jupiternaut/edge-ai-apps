package com.yourorg.craftlite.agent

import com.yourorg.craftlite.data.repo.SessionRepository
import com.yourorg.craftlite.data.repo.SettingsRepository
import com.yourorg.craftlite.data.repo.WorkspaceRepository
import com.yourorg.craftlite.skills.SkillLoader
import javax.inject.Inject

class PromptContextAssembler @Inject constructor(
  private val workspaceRepository: WorkspaceRepository,
  private val sessionRepository: SessionRepository,
  private val settingsRepository: SettingsRepository,
  private val skillLoader: SkillLoader,
) {
  suspend fun assemble(request: ConversationTurnRequest): PromptContext {
    val workspace = workspaceRepository.listWorkspaces().firstOrNull { it.id == request.workspaceId }
    val recentMessages = sessionRepository.listMessages(request.sessionId).takeLast(6)
    val permissionMode = settingsRepository.getPermissionMode()
    val skills = skillLoader.loadSkillTexts()

    return PromptContext(
      workspace = workspace,
      recentMessages = recentMessages,
      activeSkills = skills,
      permissionMode = permissionMode,
    )
  }
}
