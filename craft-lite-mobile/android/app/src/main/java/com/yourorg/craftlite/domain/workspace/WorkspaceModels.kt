package com.yourorg.craftlite.domain.workspace

data class Workspace(
  val id: String,
  val name: String,
  val rootPath: String,
)

data class WorkspaceFile(
  val path: String,
  val name: String,
  val isDirectory: Boolean,
  val sizeBytes: Long? = null,
)
