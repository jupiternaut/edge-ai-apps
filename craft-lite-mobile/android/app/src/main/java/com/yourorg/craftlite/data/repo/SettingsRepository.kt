package com.yourorg.craftlite.data.repo

import com.yourorg.craftlite.domain.permissions.PermissionMode

interface SettingsRepository {
  suspend fun getPermissionMode(): PermissionMode
  suspend fun setPermissionMode(mode: PermissionMode)
}
