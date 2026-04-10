package com.yourorg.craftlite.agent

import com.yourorg.craftlite.domain.permissions.PermissionMode
import com.yourorg.craftlite.domain.session.ChatMessage
import com.yourorg.craftlite.domain.workspace.Workspace

data class PromptContext(
  val workspace: Workspace?,
  val recentMessages: List<ChatMessage>,
  val activeSkills: List<String>,
  val permissionMode: PermissionMode,
  val relevantFileSnippets: List<String> = emptyList(),
)
