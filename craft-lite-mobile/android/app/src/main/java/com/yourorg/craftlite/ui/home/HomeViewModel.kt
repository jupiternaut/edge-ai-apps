package com.yourorg.craftlite.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourorg.craftlite.data.repo.SessionRepository
import com.yourorg.craftlite.data.repo.WorkspaceRepository
import com.yourorg.craftlite.domain.session.SessionSummary
import com.yourorg.craftlite.domain.workspace.Workspace
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val workspaceRepository: WorkspaceRepository,
  private val sessionRepository: SessionRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val workspaces = workspaceRepository.listWorkspaces()
      val sessions = sessionRepository.listSessions()
      _uiState.update {
        it.copy(
          isLoading = false,
          workspaces = workspaces,
          sessions = sessions,
        )
      }
    }
  }
}

data class HomeUiState(
  val isLoading: Boolean = false,
  val workspaces: List<Workspace> = emptyList(),
  val sessions: List<SessionSummary> = emptyList(),
)
