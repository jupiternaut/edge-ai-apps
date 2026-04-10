package com.google.ai.edge.gallery.customtasks.craftagents

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CraftAgentsViewModel @Inject constructor() : ViewModel() {
  private val _uiState = MutableStateFlow(CraftAgentsUiState())
  val uiState: StateFlow<CraftAgentsUiState> = _uiState.asStateFlow()

  fun setDraftServerUrl(value: String) {
    _uiState.update { current -> current.copy(draftServerUrl = value) }
  }

  fun connect() {
    val normalized = normalizeUrl(_uiState.value.draftServerUrl)
    if (normalized == null) {
      _uiState.update { current ->
        current.copy(error = "Enter a valid http:// or https:// Craft Agents URL.")
      }
      return
    }

    _uiState.update { current ->
      current.copy(
        error = null,
        connectedUrl = normalized,
        isLoading = true,
      )
    }
  }

  fun onPageStarted() {
    _uiState.update { current -> current.copy(isLoading = true, error = null) }
  }

  fun onPageFinished(title: String?) {
    _uiState.update { current ->
      current.copy(
        isLoading = false,
        pageTitle = title?.takeIf { it.isNotBlank() } ?: current.pageTitle,
      )
    }
  }

  fun onPageError(message: String) {
    _uiState.update { current ->
      current.copy(
        isLoading = false,
        error = message,
      )
    }
  }

  private fun normalizeUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
    return trimmed.trimEnd('/')
  }
}

data class CraftAgentsUiState(
  val draftServerUrl: String = "",
  val connectedUrl: String? = null,
  val pageTitle: String? = null,
  val isLoading: Boolean = false,
  val error: String? = null,
)
