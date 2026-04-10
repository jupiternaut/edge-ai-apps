package com.google.ai.edge.gallery.customtasks.edgecodex

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.gallery.ui.llmchat.LlmModelInstance
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
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

private const val TAG = "EdgeCodexViewModel"

@HiltViewModel
class EdgeCodexViewModel @Inject constructor() : ViewModel() {
  private val _uiState = MutableStateFlow(EdgeCodexUiState())
  val uiState: StateFlow<EdgeCodexUiState> = _uiState.asStateFlow()

  private val _isResettingConversation = MutableStateFlow(false)

  private var currentLanguage: String = "python"
  private var turnCount: Int = 0

  var config = CodexConfig()
    private set

  fun updateConfig(model: Model, newConfig: CodexConfig) {
    config = newConfig
    viewModelScope.launch(Dispatchers.Default) {
      if (model.instance != null) {
        resetConversation(model = model, code = null)
      }
    }
  }

  fun setLanguage(model: Model, language: String) {
    currentLanguage = language
    viewModelScope.launch(Dispatchers.Default) {
      if (model.instance != null) {
        resetConversation(model = model, code = null)
      }
    }
  }

  fun analyzeCode(model: Model, userMessage: String, code: String) {
    if (model.instance == null || userMessage.isBlank()) {
      Log.w(TAG, "analyzeCode skipped: instance=${model.instance != null}, blank=${userMessage.isBlank()}")
      return
    }

    viewModelScope.launch(Dispatchers.Default) {
      setProcessing(processing = true)
      clearError()
      clearLastResponse()

      _isResettingConversation.first { !it }
      val instance = model.instance as? LlmModelInstance
      if (instance == null) {
        Log.e(TAG, "model.instance is not a LlmModelInstance")
        setProcessing(processing = false)
        setError("EdgeCodex model is not initialized.")
        return@launch
      }

      val prompt =
        if (code.isNotBlank()) {
          "$userMessage\n\nCurrent code:\n```$currentLanguage\n${code.take(4000)}\n```"
        } else {
          userMessage
        }

      Log.d(TAG, "analyzeCode turn=$turnCount lang=$currentLanguage msgLen=${userMessage.length} codeLen=${code.length}")
      val responseBuilder = StringBuilder()
      try {
        instance.conversation
          .sendMessageAsync(Contents.of(listOf(Content.Text(prompt))))
          .collect { message ->
            responseBuilder.append(message.toString())
            _uiState.update { currentState ->
              currentState.copy(streamingResponse = responseBuilder.toString())
            }
          }

        val finalResponse =
          responseBuilder.toString().ifBlank { "EdgeCodex did not return a response." }
        Log.d(TAG, "analyzeCode completed, responseLen=${finalResponse.length}")
        _uiState.update { currentState ->
          currentState.copy(
            streamingResponse = null,
            lastResponse = finalResponse,
          )
        }

        turnCount += 1
        if (turnCount >= RESET_INTERVAL) {
          Log.i(TAG, "reached RESET_INTERVAL=$RESET_INTERVAL, resetting conversation")
          resetConversation(model = model, code = code)
        }
      } catch (e: Exception) {
        Log.e(TAG, "analyzeCode stream failed", e)
        setError("Analysis failed: ${e.message ?: "Unknown error"}")
      } finally {
        setProcessing(processing = false)
      }
    }
  }

  private fun resetConversation(model: Model, code: String?) {
    _isResettingConversation.value = true
    LlmChatModelHelper.resetConversation(
      model = model,
      supportImage = false,
      supportAudio = false,
      systemInstruction =
        Contents.of(
          EdgeCodexTask().buildSystemPrompt(
            language = currentLanguage,
            code = code,
            customPrompt = config.systemPrompt,
          )
        ),
      tools = emptyList(),
      enableConversationConstrainedDecoding = false,
    )
    turnCount = 0
    _isResettingConversation.value = false
    Log.d(TAG, "conversation reset complete, turnCount=0")
  }

  private fun setProcessing(processing: Boolean) {
    _uiState.update { currentState -> currentState.copy(processing = processing) }
  }

  private fun setError(error: String?) {
    _uiState.update { currentState -> currentState.copy(error = error) }
  }

  private fun clearError() {
    setError(error = null)
  }

  private fun clearLastResponse() {
    _uiState.update { currentState ->
      currentState.copy(
        streamingResponse = null,
        lastResponse = null,
      )
    }
  }

  private companion object {
    private const val RESET_INTERVAL = 10
  }
}

data class EdgeCodexUiState(
  val processing: Boolean = false,
  val error: String? = null,
  val streamingResponse: String? = null,
  val lastResponse: String? = null,
)

@Serializable
data class CodexConfig(
  val temperature: Float = 0.7f,
  val topK: Int = 64,
  val topP: Float = 0.95f,
  val maxTokens: Int = 2048,
  val systemPrompt: String = "",
)
