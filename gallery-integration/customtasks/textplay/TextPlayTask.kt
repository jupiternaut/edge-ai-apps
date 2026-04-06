package com.google.ai.edge.gallery.customtasks.textplay

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Gallery custom task for TextPlay.
 *
 * This follows the same registration and lifecycle pattern as TinyGarden:
 * - task metadata is surfaced through [Task]
 * - model init/cleanup is delegated to [LlmChatModelHelper]
 * - function-calling updates are streamed to the WebView through a buffered channel
 */
class TextPlayTask @Inject constructor() : CustomTask {
  private val _updateChannel = Channel<WebViewCommand>(Channel.BUFFERED)
  private val commandFlow = _updateChannel.receiveAsFlow()
  private val tools =
    listOf(
      tool(
        TextPlayTools(
          onAction = { action ->
            val unused = _updateChannel.trySend(action.toWebViewCommand())
          }
        )
      )
    )

  override val task: Task =
    Task(
      id = "llm_textplay",
      label = "TextPlay",
      category = Category.LLM,
      icon = Icons.Outlined.AutoAwesome,
      models = mutableListOf(),
      description =
        "Guide an offline fantasy micro-world with natural language. FunctionGemma converts player intent into on-device game actions.",
      shortDescription = "Offline text adventure",
      docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
      sourceCodeUrl =
        "https://github.com/jupiternaut/edge-ai-apps/tree/master/gallery-integration/customtasks/textplay",
      experimental = true,
      defaultSystemPrompt = buildSystemPrompt(),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    clearQueue()
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = Contents.of(buildSystemPrompt()),
      tools = tools,
      enableConversationConstrainedDecoding = true,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    clearQueue()
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    TextPlayScreen(
      modelManagerViewModel = customTaskData.modelManagerViewModel,
      bottomPadding = customTaskData.bottomPadding,
      commandFlow = commandFlow,
      tools = tools,
      setAppBarControlsDisabled = customTaskData.setAppBarControlsDisabled,
    )
  }

  private fun clearQueue() {
    while (_updateChannel.tryReceive().isSuccess) {}
  }

  fun buildSystemPrompt(gameState: TextPlayGameState? = null): String {
    val stateInfo =
      gameState?.let { state ->
        """
        |
        |Current game state:
        |- Player position: (${state.playerX}, ${state.playerY})
        |- Player HP: ${state.playerHp}
        |- Inventory: ${state.inventory.joinToString(", ").ifEmpty { "empty" }}
        |- Completed quests: ${state.completedQuests.joinToString(", ").ifEmpty { "none" }}
        |- Last action: ${state.lastAction ?: "none"}
        """
          .trimMargin()
      } ?: ""

    return """
      |You are the action planner for TextPlay, a fully offline text adventure game.
      |Translate every player instruction into function calls only.
      |
      |World summary:
      |- The map is a 16x12 tile grid. Coordinates are (x, y) with (0, 0) at the top-left.
      |- The village center is around (5-8, 5-7) and contains a campfire, sign, and well.
      |- The farm is around (3-6, 1-4) and contains berry bushes plus the farmer.
      |- The elder is near (8, 3), the merchant is near (10, 7), and the cave gate is near (12, 2-3).
      |
      |Available functions:
      |- movePlayer(direction, steps): direction must be north, south, east, or west.
      |- lookAround(): inspect surroundings or recover when intent is ambiguous.
      |- pickupItem(item): collect or harvest a nearby item.
      |- useItem(item, target): use an inventory item on a world target.
      |- talkToNPC(npc): talk to farmer, elder, or merchant.
      |- craftItems(item1, item2): combine ingredients or interact with crafting stations.
      |- checkInventory(): list carried items.
      |- interactWith(target): interact with world objects such as sign, well, chest, gate, or campfire.
      |
      |Rules:
      |- Prefer function calls over text responses.
      |- Break multi-step requests into multiple function calls in order.
      |- If a request is vague or impossible, call lookAround() instead of free-form dialogue.
      |$stateInfo
      """
      .trimMargin()
  }
}
