package com.google.ai.edge.gallery.customtasks.edgecodex

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Gallery custom task for EdgeCodex.
 *
 * Unlike TextPlay, this task runs Gemma 4 E2B in regular conversational mode without tools.
 */
class EdgeCodexTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = "llm_edgecodex",
      label = "EdgeCodex",
      category = Category.LLM,
      icon = Icons.Outlined.Code,
      models = mutableListOf(),
      description =
        "Analyze, explain, refactor, and generate code entirely on-device with Gemma 4 E2B inside a lightweight IDE.",
      shortDescription = "On-device code assistant",
      docUrl = "https://github.com/google-ai-edge/LiteRT-LM/blob/main/kotlin/README.md",
      sourceCodeUrl =
        "https://github.com/jupiternaut/edge-ai-apps/tree/master/gallery-integration/customtasks/edgecodex",
      experimental = true,
      defaultSystemPrompt = buildSystemPrompt(),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = false,
      supportAudio = false,
      onDone = onDone,
      systemInstruction = Contents.of(buildSystemPrompt()),
      tools = emptyList(),
      enableConversationConstrainedDecoding = false,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    EdgeCodexScreen(
      modelManagerViewModel = customTaskData.modelManagerViewModel,
      bottomPadding = customTaskData.bottomPadding,
      setAppBarControlsDisabled = customTaskData.setAppBarControlsDisabled,
    )
  }

  fun buildSystemPrompt(
    language: String = "python",
    code: String? = null,
    customPrompt: String? = null,
  ): String {
    val codeContext =
      code?.takeIf { it.isNotBlank() }?.let {
        """
        |
        |== CURRENT CODE (${language}) ==
        |```$language
        |${it.take(4000)}
        |```
        """
          .trimMargin()
      } ?: ""

    val basePrompt = customPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_SYSTEM_PROMPT
    return """
      |$basePrompt
      |
      |== ACTIVE LANGUAGE ==
      |$language
      |
      |== RESPONSE FORMAT ==
      |1. Lead with the answer, not the reasoning.
      |2. Use markdown code fences with language tags when showing code.
      |3. Prefer concrete fixes, refactors, or tests over generic advice.
      |4. Keep the explanation concise and end with a one-line summary.
      |$codeContext
      """
      .trimMargin()
  }

  private companion object {
    private const val DEFAULT_SYSTEM_PROMPT =
      """You are EdgeCodex, an expert software engineering assistant running entirely on-device via Gemma 4 E2B.
Your strengths include:
- code explanation and walkthroughs
- bug finding and fixes
- refactoring guidance
- completion and implementation suggestions
- unit test generation
- performance analysis

The user's source code stays on the device. Provide practical, actionable answers with code when useful."""
  }
}
