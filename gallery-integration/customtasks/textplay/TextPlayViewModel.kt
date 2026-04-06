package com.google.ai.edge.gallery.customtasks.textplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ToolProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@HiltViewModel
class TextPlayViewModel @Inject constructor() : ViewModel() {
  private val _uiState = MutableStateFlow(TextPlayUiState())
  val uiState: StateFlow<TextPlayUiState> = _uiState.asStateFlow()

  private val _isResettingConversation = MutableStateFlow(false)
  private var turnCount = 0
  private var lastGameState: TextPlayGameState? = null

  fun processUserInput(model: Model, input: String, tools: List<ToolProvider>) {
    if (model.instance == null || input.isBlank()) {
      return
    }

    viewModelScope.launch(Dispatchers.Default) {
      setProcessing(processing = true)
      clearError()

      _isResettingConversation.first { !it }
      val instance = model.instance as? LlmModelInstance
      if (instance == null) {
        setProcessing(processing = false)
        setError("TextPlay model is not initialized.")
        return@launch
      }

      try {
        val contents = listOf(Content.Text(input.trim()))
        instance.conversation.sendMessage(Contents.of(contents))
        turnCount += 1

        if (turnCount >= RESET_INTERVAL) {
          resetConversation(model = model, tools = tools)
        }
      } catch (e: Exception) {
        setError(e.message ?: "Failed to process the TextPlay command.")
      } finally {
        setProcessing(processing = false)
      }
    }
  }

  fun updateGameState(rawStateJson: String) {
    val normalizedJson =
      runCatching { Json.decodeFromString<String>(rawStateJson) }
        .getOrElse { rawStateJson.trim().removePrefix("\"").removeSuffix("\"") }

    runCatching { Json.decodeFromString<TextPlayGameState>(normalizedJson) }
      .onSuccess { gameState -> lastGameState = gameState }
  }

  private fun resetConversation(model: Model, tools: List<ToolProvider>) {
    _isResettingConversation.value = true
    setResettingEngine(resetting = true)
    LlmChatModelHelper.resetConversation(
      model = model,
      supportImage = false,
      supportAudio = false,
      systemInstruction = Contents.of(TextPlayTask().buildSystemPrompt(gameState = lastGameState)),
      tools = tools,
      enableConversationConstrainedDecoding = true,
    )
    turnCount = 0
    setResettingEngine(resetting = false)
    _isResettingConversation.value = false
  }

  private fun setProcessing(processing: Boolean) {
    _uiState.update { currentState -> currentState.copy(processing = processing) }
  }

  private fun setResettingEngine(resetting: Boolean) {
    _uiState.update { currentState -> currentState.copy(resettingEngine = resetting) }
  }

  private fun setError(error: String?) {
    _uiState.update { currentState -> currentState.copy(error = error) }
  }

  private fun clearError() {
    setError(error = null)
  }

  companion object {
    private const val RESET_INTERVAL = 15
  }
}

data class TextPlayUiState(
  val processing: Boolean = false,
  val resettingEngine: Boolean = false,
  val error: String? = null,
)

@Serializable
data class WebViewCommand(
  val action: String,
  val direction: String? = null,
  val steps: Int? = null,
  val item: String? = null,
  val target: String? = null,
  val npc: String? = null,
  val item1: String? = null,
  val item2: String? = null,
  val text: String? = null,
  val type: String? = null,
)

@Serializable
data class TextPlayGameState(
  val playerX: Int,
  val playerY: Int,
  val playerHp: Int,
  val inventory: List<String>,
  val completedQuests: List<String>,
  val lastAction: String? = null,
)
