package com.google.ai.edge.gallery.customtasks.craftagents

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.CategoryInfo
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Remote Craft Agents client for AI Edge Gallery.
 *
 * This task does not run Craft Agents on-device.
 * It uses Gallery's CustomTask surface to host a WebView that connects to a
 * separately deployed Craft Agents server with Web UI enabled.
 */
class CraftAgentsTask @Inject constructor() : CustomTask {
  override val task: Task =
    Task(
      id = TASK_ID,
      label = "Craft Agents",
      category = CategoryInfo(id = "connected_apps", label = "Connected Apps"),
      icon = Icons.Outlined.Language,
      description =
        "Launch Craft Agents as a remote thin client inside AI Edge Gallery. " +
          "Requires a reachable Craft Agents server with Web UI enabled.",
      shortDescription = "Remote agent workspace in WebView",
      docUrl =
        "https://github.com/lukilabs/craft-agents-oss",
      sourceCodeUrl =
        "https://github.com/jupiternaut/edge-ai-apps/tree/master/gallery-integration/customtasks/craftagents",
      models =
        mutableListOf(
          Model(
            name = "Craft Agents Remote Client",
            info =
              "Bootstrap profile for the Craft Agents remote WebView task. " +
                "This placeholder asset satisfies Gallery's model lifecycle.",
            url = "https://raw.githubusercontent.com/lukilabs/craft-agents-oss/main/README.md",
            sizeInBytes = 30000L,
            downloadFileName = "craft-agents-bootstrap.txt",
            bestForTaskIds = listOf(TASK_ID),
          ),
        ),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    coroutineScope.launch(Dispatchers.IO) {
      model.instance = null
      try {
        val bootstrapFile = File(model.getPath(context = context))
        if (!bootstrapFile.exists()) {
          onDone("Craft Agents bootstrap file is missing.")
          return@launch
        }

        model.instance = CraftAgentsModelInstance(bootstrapPath = bootstrapFile.absolutePath)
        onDone("")
      } catch (e: Exception) {
        onDone(e.message ?: "Failed to initialize Craft Agents task.")
      }
    }
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    model.instance = null
    onDone()
  }

  @Composable
  override fun MainScreen(data: Any) {
    val customTaskData = data as CustomTaskData
    CraftAgentsScreen(
      modelManagerViewModel = customTaskData.modelManagerViewModel,
      bottomPadding = customTaskData.bottomPadding,
      setAppBarControlsDisabled = customTaskData.setAppBarControlsDisabled,
    )
  }

  private companion object {
    private const val TASK_ID = "connected_craft_agents"
  }
}

data class CraftAgentsModelInstance(
  val bootstrapPath: String,
)
