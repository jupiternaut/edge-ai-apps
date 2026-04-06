package com.google.ai.edge.gallery.customtasks.textplay

import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData

/**
 * TextPlay task definition.
 * Configures the FunctionGemma model with game context and system prompt.
 * Follows the same pattern as TinyGardenTask in AI Edge Gallery.
 */
object TextPlayTask : CustomTask {

    override val taskType: String = "llm_textplay"
    override val displayName: String = "TextPlay"
    override val description: String = "AI-driven text adventure sandbox game"

    /**
     * System prompt that teaches FunctionGemma the game's world layout,
     * available actions, spatial mapping, and response conventions.
     *
     * Updated dynamically with game state on conversation resets.
     */
    fun buildSystemPrompt(gameState: TextPlayGameState? = null): String {
        val stateInfo = gameState?.let { state ->
            """
            |
            |Current game state:
            |  Player position: (${state.playerX}, ${state.playerY})
            |  Player HP: ${state.playerHp}
            |  Inventory: ${state.inventory.joinToString(", ").ifEmpty { "empty" }}
            |  Completed quests: ${state.completedQuests.joinToString(", ").ifEmpty { "none" }}
            |  Last action: ${state.lastAction ?: "none"}
            """.trimMargin()
        } ?: ""

        return """
        |You are the AI controller for TextPlay, a text-driven sandbox adventure game.
        |Your ONLY job is to translate the player's natural language into function calls.
        |
        |== GAME WORLD ==
        |The map is a 16x12 tile grid. Coordinates are (x, y) where (0,0) is top-left.
        |Terrain: grass (walkable), path (walkable), trees (blocked), water (blocked), rocks (blocked).
        |
        |Key locations:
        |  - Village center: around (5-8, 5-7), has campfire, sign, well
        |  - Farm area: around (3-6, 1-4), has berry bushes, farmer NPC
        |  - Cave entrance: around (12, 2-3), has locked gate, old chest
        |  - Lake: around (7-8, 8-9), water tiles
        |  - Elder's area: around (8, 3)
        |  - Merchant: around (10, 7)
        |
        |== SPATIAL REFERENCES ==
        |  "north" / "up" = negative Y direction
        |  "south" / "down" = positive Y direction
        |  "east" / "right" = positive X direction
        |  "west" / "left" = negative X direction
        |
        |== AVAILABLE ACTIONS ==
        |You may call these functions based on the player's intent:
        |  - movePlayer(direction, steps): Move the player. direction is "north"/"south"/"east"/"west". steps defaults to 1.
        |  - lookAround(): Describe the player's surroundings.
        |  - pickupItem(item): Pick up or harvest an item. item is the name like "berry", "herb", "chest", "torch".
        |  - useItem(item, target): Use an inventory item on a target. e.g. useItem("rusty_key", "gate").
        |  - talkToNPC(npc): Talk to an NPC. npc is "farmer", "elder", or "merchant".
        |  - craftItems(item1, item2): Combine items. e.g. craftItems("berry", "campfire") to cook.
        |  - checkInventory(): List the player's inventory.
        |  - interactWith(target): Generic interact with an object like "sign", "well".
        |
        |== RULES ==
        |1. ALWAYS call a function. Never reply with just text.
        |2. If the player's intent is unclear, call lookAround() to give them context.
        |3. Map multi-step requests to multiple function calls in sequence.
        |4. If the player says something impossible (like "fly"), do NOT call any function. Instead return a brief text explanation.
        |$stateInfo
        """.trimMargin()
    }

    override fun getData(): CustomTaskData {
        return CustomTaskData(
            taskType = taskType,
            webAssetPath = "textplay/index.html",
            systemPrompt = buildSystemPrompt(),
            modelConfig = mapOf(
                "topK" to 64,
                "topP" to 0.95,
                "temperature" to 1.0,
                "maxTokens" to 512,
            ),
        )
    }
}

/**
 * Snapshot of the current game state, used to refresh the system prompt
 * on conversation resets so the model maintains context.
 */
data class TextPlayGameState(
    val playerX: Int,
    val playerY: Int,
    val playerHp: Int,
    val inventory: List<String>,
    val completedQuests: List<String>,
    val lastAction: String?,
)
