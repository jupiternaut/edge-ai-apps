package com.google.ai.edge.gallery.customtasks.textplay

import android.util.Log
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

private const val TAG = "TextPlayTools"

/**
 * FunctionGemma tool definitions for TextPlay.
 *
 * Each @Tool method is automatically registered with LiteRT-LM and exposed
 * to the model. When FunctionGemma parses user input, it generates structured
 * calls to these methods, which then emit TextPlayAction objects via the callback.
 *
 * Pattern follows TinyGardenTools.kt from AI Edge Gallery.
 */
class TextPlayTools(
    private val onAction: (TextPlayAction) -> Unit,
) : ToolSet {

    @Tool(description = "Move the player character in a direction. Valid directions: north, south, east, west.")
    fun movePlayer(
        @ToolParam(description = "Direction to move: north, south, east, or west") direction: String,
        @ToolParam(description = "Number of steps to move (default 1, max 5)") steps: Int = 1,
    ): Map<String, Any> {
        Log.d(TAG, "movePlayer called: direction=$direction, steps=$steps")
        onAction(TextPlayAction.Move(direction, steps.coerceIn(1, 5)))
        return mapOf("result" to "success")
    }

    @Tool(description = "Look around and describe the player's current surroundings, nearby objects, and NPCs.")
    fun lookAround(): Map<String, Any> {
        Log.d(TAG, "lookAround called")
        onAction(TextPlayAction.Look)
        return mapOf("result" to "success")
    }

    @Tool(description = "Pick up, harvest, or collect an item from the environment.")
    fun pickupItem(
        @ToolParam(description = "Name of the item to pick up, e.g. 'berry', 'herb', 'chest', 'torch'") item: String,
    ): Map<String, Any> {
        Log.d(TAG, "pickupItem called: item=$item")
        onAction(TextPlayAction.Pickup(item))
        return mapOf("result" to "success")
    }

    @Tool(description = "Use an inventory item on a target object in the environment.")
    fun useItem(
        @ToolParam(description = "Name of the item from inventory to use") item: String,
        @ToolParam(description = "Name of the target to use the item on, e.g. 'gate', 'campfire'") target: String,
    ): Map<String, Any> {
        Log.d(TAG, "useItem called: item=$item, target=$target")
        onAction(TextPlayAction.UseItem(item, target))
        return mapOf("result" to "success")
    }

    @Tool(description = "Talk to an NPC (non-player character) in the game world.")
    fun talkToNPC(
        @ToolParam(description = "Name of the NPC to talk to, e.g. 'farmer', 'elder', 'merchant'") npc: String,
    ): Map<String, Any> {
        Log.d(TAG, "talkToNPC called: npc=$npc")
        onAction(TextPlayAction.Talk(npc))
        return mapOf("result" to "success")
    }

    @Tool(description = "Craft or combine two items together. Must be near a crafting station if required.")
    fun craftItems(
        @ToolParam(description = "First item or ingredient") item1: String,
        @ToolParam(description = "Second item, ingredient, or crafting station") item2: String,
    ): Map<String, Any> {
        Log.d(TAG, "craftItems called: item1=$item1, item2=$item2")
        onAction(TextPlayAction.Craft(item1, item2))
        return mapOf("result" to "success")
    }

    @Tool(description = "Check the player's current inventory.")
    fun checkInventory(): Map<String, Any> {
        Log.d(TAG, "checkInventory called")
        onAction(TextPlayAction.CheckInventory)
        return mapOf("result" to "success")
    }

    @Tool(description = "Interact with a nearby object such as a sign, well, or chest.")
    fun interactWith(
        @ToolParam(description = "Name of the object to interact with") target: String,
    ): Map<String, Any> {
        Log.d(TAG, "interactWith called: target=$target")
        onAction(TextPlayAction.Interact(target))
        return mapOf("result" to "success")
    }
}

/**
 * Sealed class representing all possible game actions.
 * Emitted by TextPlayTools, consumed by TextPlayViewModel.
 */
sealed class TextPlayAction {
    data class Move(val direction: String, val steps: Int = 1) : TextPlayAction()
    data object Look : TextPlayAction()
    data class Pickup(val item: String) : TextPlayAction()
    data class UseItem(val item: String, val target: String) : TextPlayAction()
    data class Talk(val npc: String) : TextPlayAction()
    data class Craft(val item1: String, val item2: String) : TextPlayAction()
    data object CheckInventory : TextPlayAction()
    data class Interact(val target: String) : TextPlayAction()
}

fun TextPlayAction.toWebViewCommand(): WebViewCommand {
    return when (this) {
        is TextPlayAction.Move -> WebViewCommand(action = "move", direction = direction, steps = steps)
        TextPlayAction.Look -> WebViewCommand(action = "look")
        is TextPlayAction.Pickup -> WebViewCommand(action = "pickup", item = item)
        is TextPlayAction.UseItem -> WebViewCommand(action = "use", item = item, target = target)
        is TextPlayAction.Talk -> WebViewCommand(action = "talk", npc = npc)
        is TextPlayAction.Craft -> WebViewCommand(action = "craft", item1 = item1, item2 = item2)
        TextPlayAction.CheckInventory -> WebViewCommand(action = "inventory")
        is TextPlayAction.Interact -> WebViewCommand(action = "interact", target = target)
    }
}
