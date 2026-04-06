package com.google.ai.edge.gallery.customtasks.edgecodex

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.backend.Backend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for EdgeCodex code assistant.
 *
 * Uses Gemma 4 E2B in conversational (non-function-calling) mode.
 * The model receives the user's code + question and returns free-form
 * markdown responses with code suggestions.
 *
 * Key differences from TextPlay:
 * - No @Tool functions (free-form conversation, not structured actions)
 * - Uses streaming responses for better UX
 * - Supports Prompt Lab parameter tuning
 * - Larger context window for code analysis
 */
@HiltViewModel
class EdgeCodexViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EdgeCodexUiState())
    val uiState: StateFlow<EdgeCodexUiState> = _uiState.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentLanguage: String = "python"
    private var turnCount: Int = 0

    // ==================== Prompt Lab Config ====================
    var config = CodexConfig()
        private set

    fun updateConfig(newConfig: CodexConfig) {
        config = newConfig
        // Recreate conversation with new parameters
        resetConversation(null)
    }

    // ==================== ENGINE LIFECYCLE ====================

    fun initializeEngine(context: Context, model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(loading = true)

            try {
                val engineConfig = EngineConfig(
                    modelPath = model.getPath(context),
                    backend = Backend.GPU(),
                    maxNumTokens = config.maxTokens,
                )
                val newEngine = Engine(engineConfig).apply { initialize() }

                val samplerConfig = SamplerConfig(
                    topK = config.topK,
                    topP = config.topP,
                    temperature = config.temperature,
                )

                val conv = newEngine.createConversation(
                    ConversationConfig(
                        samplerConfig = samplerConfig,
                        systemInstruction = Contents.of(
                            EdgeCodexTask.buildSystemPrompt(
                                language = currentLanguage,
                                customPrompt = config.systemPrompt.ifBlank { null },
                            )
                        ),
                    )
                )

                engine = newEngine
                conversation = conv

                _uiState.value = _uiState.value.copy(loading = false)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Failed to load model: ${e.message}",
                )
            }
        }
    }

    // ==================== CODE ANALYSIS ====================

    /**
     * Send user's question + current code to Gemma 4 E2B.
     * Uses streaming for real-time response display.
     */
    fun analyzeCode(userMessage: String, code: String) {
        val conv = conversation ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(processing = true)

            // Build message with code context
            val fullMessage = if (code.isNotBlank()) {
                "$userMessage\n\nCurrent code:\n```${currentLanguage}\n${code.take(4000)}\n```"
            } else {
                userMessage
            }

            try {
                // Stream response tokens
                val responseBuilder = StringBuilder()

                conv.sendMessageAsync(Contents.of(fullMessage)).collect { message ->
                    val text = message.toString()
                    responseBuilder.append(text)

                    // Update UI with streaming text
                    _uiState.value = _uiState.value.copy(
                        streamingResponse = responseBuilder.toString(),
                    )
                }

                // Finalize response
                val finalResponse = responseBuilder.toString()
                _uiState.value = _uiState.value.copy(
                    streamingResponse = null,
                    lastResponse = finalResponse,
                )

                // Check if thinking mode was used
                // Gemma 4's thinking channel provides reasoning steps
                // message.channels["thought"] contains the thinking process

                turnCount++
                if (turnCount >= RESET_INTERVAL) {
                    resetConversation(code)
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Analysis failed: ${e.message}",
                )
            }

            _uiState.value = _uiState.value.copy(processing = false)
        }
    }

    // ==================== LANGUAGE & CONTEXT ====================

    fun setLanguage(language: String) {
        currentLanguage = language
    }

    private fun resetConversation(code: String?) {
        viewModelScope.launch(Dispatchers.Default) {
            conversation?.close()

            val samplerConfig = SamplerConfig(
                topK = config.topK,
                topP = config.topP,
                temperature = config.temperature,
            )

            conversation = engine?.createConversation(
                ConversationConfig(
                    samplerConfig = samplerConfig,
                    systemInstruction = Contents.of(
                        EdgeCodexTask.buildSystemPrompt(
                            language = currentLanguage,
                            code = code,
                            customPrompt = config.systemPrompt.ifBlank { null },
                        )
                    ),
                )
            )
            turnCount = 0
        }
    }

    override fun onCleared() {
        conversation?.close()
        engine?.close()
        super.onCleared()
    }

    companion object {
        private const val RESET_INTERVAL = 10
    }
}

// ==================== DATA CLASSES ====================

data class EdgeCodexUiState(
    val loading: Boolean = false,
    val processing: Boolean = false,
    val error: String? = null,
    val streamingResponse: String? = null,
    val lastResponse: String? = null,
)

data class CodexConfig(
    val temperature: Float = 0.7f,
    val topK: Int = 64,
    val topP: Float = 0.95f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "",
)
