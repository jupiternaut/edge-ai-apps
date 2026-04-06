package com.google.ai.edge.gallery.customtasks.textplay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.runtime.LlmChatModelHelper
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.tools.tool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * ViewModel for TextPlay game.
 *
 * Manages:
 * - LiteRT-LM engine lifecycle (FunctionGemma for actions, Gemma 4 for dialogue)
 * - Game state tracking and conversation resets
 * - Communication bridge between model inference and WebView game
 *
 * Pattern follows TinyGardenViewModel.kt from AI Edge Gallery.
 */
@HiltViewModel
class TextPlayViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TextPlayUiState())
    val uiState: StateFlow<TextPlayUiState> = _uiState.asStateFlow()

    /** Channel for commands to be executed in the WebView */
    private val _commandChannel = Channel<WebViewCommand>(Channel.BUFFERED)
    val commandFlow = _commandChannel.receiveAsFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var turnCount = 0
    private var lastGameState: TextPlayGameState? = null

    companion object {
        /** Reset conversation every N turns to prevent context overflow */
        private const val RESET_INTERVAL = 15
    }

    // ==================== ENGINE LIFECYCLE ====================

    /**
     * Initialize the LiteRT-LM engine with FunctionGemma model.
     * Called once when the screen is first composed.
     */
    fun initializeEngine(context: Context, model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(loading = true)

            val tools = listOf(
                tool(TextPlayTools(onAction = { action -> handleAction(action) }))
            )

            LlmChatModelHelper.initialize(
                context = context,
                model = model,
                supportImage = false,
                supportAudio = true, // Voice commands
                onDone = { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = error.ifEmpty { null },
                    )
                },
                systemInstruction = Contents.of(TextPlayTask.buildSystemPrompt()),
                tools = tools,
                enableConversationConstrainedDecoding = true,
            )

            engine = model.instance?.engine
            conversation = model.instance?.conversation
        }
    }

    /**
     * Process user's natural language input through FunctionGemma.
     */
    fun processUserInput(input: String) {
        val conv = conversation ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(processing = true)

            // Add user message to UI
            addChatMessage(ChatMessage(text = input, type = ChatMessageType.USER))

            try {
                // Send to FunctionGemma - it will trigger @Tool methods
                val response = conv.sendMessage(Contents.of(input))
                val responseText = response.toString()

                // If model returned text instead of function call, show as dialogue
                if (responseText.isNotBlank()) {
                    sendThinkingMessage(responseText)
                }
            } catch (e: Exception) {
                addChatMessage(ChatMessage(
                    text = "Oops, something went wrong: ${e.message}",
                    type = ChatMessageType.ERROR,
                ))
            }

            turnCount++
            checkConversationReset()
            _uiState.value = _uiState.value.copy(processing = false)
        }
    }

    // ==================== ACTION HANDLING ====================

    /**
     * Converts a TextPlayAction into a WebView command JSON string.
     * Called by TextPlayTools when FunctionGemma triggers a function.
     */
    private fun handleAction(action: TextPlayAction) {
        val command = when (action) {
            is TextPlayAction.Move -> WebViewCommand(
                action = "move",
                direction = action.direction,
                steps = action.steps,
            )
            is TextPlayAction.Look -> WebViewCommand(action = "look")
            is TextPlayAction.Pickup -> WebViewCommand(action = "pickup", item = action.item)
            is TextPlayAction.UseItem -> WebViewCommand(
                action = "use",
                item = action.item,
                target = action.target,
            )
            is TextPlayAction.Talk -> WebViewCommand(action = "talk", npc = action.npc)
            is TextPlayAction.Craft -> WebViewCommand(
                action = "craft",
                item1 = action.item1,
                item2 = action.item2,
            )
            is TextPlayAction.CheckInventory -> WebViewCommand(action = "inventory")
            is TextPlayAction.Interact -> WebViewCommand(action = "interact", target = action.target)
        }

        viewModelScope.launch {
            _commandChannel.send(command)
        }
    }

    /**
     * Show Gemma 4 "thinking mode" dialogue when the model can't map to a function.
     */
    private fun sendThinkingMessage(text: String) {
        viewModelScope.launch {
            _commandChannel.send(
                WebViewCommand(action = "message", text = text, type = "think")
            )
        }
    }

    // ==================== CONVERSATION MANAGEMENT ====================

    /**
     * Periodically reset the conversation to prevent context overflow.
     * Injects current game state into the fresh system prompt.
     */
    private fun checkConversationReset() {
        if (turnCount % RESET_INTERVAL != 0) return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(resettingEngine = true)

            val gameState = lastGameState
            conversation?.close()
            conversation = engine?.createConversation(
                com.google.ai.edge.litertlm.ConversationConfig(
                    systemInstruction = Contents.of(TextPlayTask.buildSystemPrompt(gameState)),
                    tools = listOf(
                        tool(TextPlayTools(onAction = { action -> handleAction(action) }))
                    ),
                )
            )

            _uiState.value = _uiState.value.copy(resettingEngine = false)
        }
    }

    /**
     * Called from WebView via JavaScript interface to sync game state.
     */
    fun updateGameState(stateJson: String) {
        try {
            lastGameState = Json.decodeFromString<TextPlayGameState>(stateJson)
        } catch (_: Exception) { }
    }

    // ==================== UI STATE ====================

    private fun addChatMessage(message: ChatMessage) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message,
        )
    }

    override fun onCleared() {
        conversation?.close()
        engine?.close()
        super.onCleared()
    }
}

// ==================== DATA CLASSES ====================

data class TextPlayUiState(
    val loading: Boolean = false,
    val processing: Boolean = false,
    val resettingEngine: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
)

data class ChatMessage(
    val text: String,
    val type: ChatMessageType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ChatMessageType { USER, SYSTEM, ACTION, NPC, ERROR, THINK }

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
