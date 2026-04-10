package com.yourorg.craftlite.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SessionScreen(
  viewModel: SessionViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Session", style = MaterialTheme.typography.headlineSmall)
    Text("Phase 2 session orchestration with turn state and cancellation.")

    OutlinedTextField(
      value = uiState.draftMessage,
      onValueChange = viewModel::setDraftMessage,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Message") },
      placeholder = { Text("Ask Craft-lite Mobile to inspect a file") },
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = viewModel::sendMessage) {
        Text("Send")
      }
      Button(onClick = viewModel::cancelTurn) {
        Text("Cancel")
      }
    }

    Text("Turn state: ${uiState.streamingState.turnState.name}", style = MaterialTheme.typography.bodyMedium)
    uiState.lastUserMessage?.let {
      Text("Last user message: $it", style = MaterialTheme.typography.bodyMedium)
    }
    if (uiState.messages.isNotEmpty()) {
      Text("Stored messages: ${uiState.messages.size}", style = MaterialTheme.typography.bodyMedium)
    }
    uiState.streamingState.status?.let {
      Text("Status: $it", style = MaterialTheme.typography.bodyMedium)
    }
    if (uiState.streamingState.text.isNotBlank()) {
      Text(uiState.streamingState.text, style = MaterialTheme.typography.bodyLarge)
    }
    uiState.streamingState.error?.let {
      Text(it, color = MaterialTheme.colorScheme.error)
    }
  }
}
