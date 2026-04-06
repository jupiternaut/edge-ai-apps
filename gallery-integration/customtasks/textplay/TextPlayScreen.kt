package com.google.ai.edge.gallery.customtasks.textplay

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.webkit.WebViewAssetLoader
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.ToolProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TextPlayScreen(
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  commandFlow: Flow<WebViewCommand>,
  tools: List<ToolProvider>,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  viewModel: TextPlayViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]
  val modelReady =
    selectedModel.instance != null &&
      modelInitializationStatus?.status != ModelInitializationStatusType.INITIALIZING

  var inputText by remember { mutableStateOf("") }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  LaunchedEffect(uiState.processing, uiState.resettingEngine) {
    setAppBarControlsDisabled(uiState.processing || uiState.resettingEngine)
  }

  DisposableEffect(Unit) { onDispose { setAppBarControlsDisabled(false) } }

  LaunchedEffect(webViewRef, commandFlow) {
    val webView = webViewRef ?: return@LaunchedEffect
    commandFlow.collect { command ->
      val json = Json.encodeToString(listOf(command))
      val escapedJson = json.replace("\\", "\\\\").replace("'", "\\'")
      webView.evaluateJavascript("textPlay.runCommands('$escapedJson')") {
        webView.evaluateJavascript("textPlay.getGameState()") { stateJson ->
          if (stateJson != null && stateJson != "null") {
            viewModel.updateGameState(stateJson)
          }
        }
      }
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(bottom = bottomPadding)
  ) {
    if (!modelReady) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          CircularProgressIndicator()
          Text("Loading FunctionGemma 270M for TextPlay...")
        }
      }
      return
    }

    AndroidView(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth(),
      factory = { context ->
        WebView(context).apply {
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.mediaPlaybackRequiresUserGesture = false

          val assetLoader =
            WebViewAssetLoader.Builder()
              .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
              .build()

          webViewClient =
            object : WebViewClient() {
              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest,
              ) = assetLoader.shouldInterceptRequest(request.url)
            }

          webChromeClient =
            object : WebChromeClient() {
              override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                android.util.Log.d(
                  "TextPlay",
                  "${consoleMessage.message()} [${consoleMessage.lineNumber()}]",
                )
                return true
              }
            }

          loadUrl("https://appassets.androidplatform.net/assets/textplay/index.html")
          webViewRef = this
        }
      },
    )

    uiState.error?.let { error ->
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }

    if (uiState.processing || uiState.resettingEngine) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Tell your character what to do...") },
        singleLine = true,
        enabled = !uiState.processing,
      )
      Spacer(modifier = Modifier.width(8.dp))
      IconButton(
        onClick = {
          val userInput = inputText.trim()
          if (userInput.isNotEmpty()) {
            inputText = ""
            viewModel.processUserInput(model = selectedModel, input = userInput, tools = tools)
          }
        },
        enabled = !uiState.processing && inputText.isNotBlank(),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send command",
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
  }
}
