package com.yourorg.craftlite.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeScreen(
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
  ) {
    Text("Craft-lite Mobile", style = MaterialTheme.typography.headlineSmall)
    Text(
      "Phase 1 scaffold wired to in-memory repositories.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text("Model: ${uiState.modelStatus}", style = MaterialTheme.typography.bodyMedium)

    if (uiState.isLoading) {
      CircularProgressIndicator()
    }

    Text("Workspaces: ${uiState.workspaces.size}", style = MaterialTheme.typography.titleMedium)
    uiState.workspaces.forEach { workspace ->
      Text("- ${workspace.name} (${workspace.rootPath})")
    }

    Text("Sessions: ${uiState.sessions.size}", style = MaterialTheme.typography.titleMedium)
    uiState.sessions.forEach { session ->
      Text("- ${session.title}")
    }
  }
}
