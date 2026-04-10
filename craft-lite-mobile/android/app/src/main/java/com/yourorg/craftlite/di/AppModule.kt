package com.yourorg.craftlite.di

import com.yourorg.craftlite.data.repo.InMemorySessionRepository
import com.yourorg.craftlite.data.repo.InMemorySettingsRepository
import com.yourorg.craftlite.data.repo.InMemoryWorkspaceRepository
import com.yourorg.craftlite.data.repo.SessionRepository
import com.yourorg.craftlite.data.repo.SettingsRepository
import com.yourorg.craftlite.data.repo.WorkspaceRepository
import com.yourorg.craftlite.llm.GemmaLiteRtEngine
import com.yourorg.craftlite.llm.LlmEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
  @Binds
  @Singleton
  abstract fun bindSessionRepository(impl: InMemorySessionRepository): SessionRepository

  @Binds
  @Singleton
  abstract fun bindWorkspaceRepository(impl: InMemoryWorkspaceRepository): WorkspaceRepository

  @Binds
  @Singleton
  abstract fun bindSettingsRepository(impl: InMemorySettingsRepository): SettingsRepository

  @Binds
  @Singleton
  abstract fun bindLlmEngine(impl: GemmaLiteRtEngine): LlmEngine
}
