package com.google.ai.edge.gallery.customtasks.textplay

import android.util.Log
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

private const val TAG = "TextPlayViewModel"

@HiltViewModel
class TextPlayViewModel @Inject constructor() : ViewModel() {
  private val _uiState = MutableStateFlow(TextPlayUiState())
  val uiState: StateFlow<TextPlayUiState> = _uiState.asStateFlow()

  private val _isResettingConversation = MutableStateFlow(false)
  private var turnCount = 0
  private var lastGameState: TextPlayGameState? = null

  fun processUserInput(model: Model, input: String, tools: List<ToolProvider>) {
    if (model.instance == null || input.isBlank()) {
      Log.w(TAG, "processUserInput skipped: instance=${model.instance != null}, blank=${input.isBlank()}")
      return
    }

    viewModelScope.launch(Dispatchers.Default) {
      setProcessing(processing = true)
      clearError()

      _isResettingConversation.first { !it }
      val instance = model.instance as? LlmModelInstance
      if (instance == null) {
        Log.e(TAG, "model.instance is not a LlmModelInstance")
        setProcessing(processing = false)
        setError("TextPlay model is not initialized.")
        return@launch
      }

      try {
        Log.d(TAG, "sendMessage turn=$turnCount input=\"${input.trim().take(80)}\"")
        val contents = listOf(Content.Text(input.trim()))
        instance.conversation.sendMessage(Contents.of(contents))
        turnCount += 1

        if (turnCount >= RESET_INTERVAL) {
          Log.i(TAG, "reached RESET_INTERVAL=$RESET_INTERVAL, resetting conversation")
          resetConversation(model = model, tools = tools)
        }
      } catch (e: Exception) {
        Log.e(TAG, "processUserInput failed", e)
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
      .onSuccess { gameState ->
        lastGameState = gameState
        Log.d(TAG, "gameState updated: pos=(${gameState.playerX},${gameState.playerY}) hp=${gameState.playerHp}")
      }
      .onFailure { Log.w(TAG, "updateGameState failed to parse JSON: ${it.message}") }
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
    Log.d(TAG, "conversation reset complete, turnCount=0")
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
