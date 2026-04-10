package com.google.ai.edge.gallery.customtasks.edgecodex

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.webkit.WebViewAssetLoader
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.serialization.json.Json

private const val EDGE_CODEX_ANDROID_BRIDGE_SCRIPT =
  """
  (function() {
    if (!window.edgeCodex || !edgeCodex.app || edgeCodex.__androidBridgeInstalled) {
      return;
    }

    edgeCodex.__androidBridgeInstalled = true;
    const originalHandleUserMessage = edgeCodex.app.handleUserMessage.bind(edgeCodex.app);
    const originalShowResponse = edgeCodex.showResponse.bind(edgeCodex);

    edgeCodex.app.handleUserMessage = function(text) {
      if (!(window.Android && Android.analyzeCode)) {
        originalHandleUserMessage(text);
        return;
      }

      this.addMessage('user', text);
      state.processing = true;

      if (Android.setLanguage) {
        Android.setLanguage(state.language);
      }
      if (Android.updateConfig) {
        Android.updateConfig(JSON.stringify(state.config));
      }

      Android.analyzeCode(text, this.editor.value);
    };

    edgeCodex.showResponse = function(text, role) {
      state.processing = false;
      originalShowResponse(text, role || 'assistant');
    };
  })();
  """

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EdgeCodexScreen(
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  viewModel: EdgeCodexViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]
  val modelReady =
    selectedModel.instance != null &&
      modelInitializationStatus?.status != ModelInitializationStatusType.INITIALIZING

  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  LaunchedEffect(uiState.processing) { setAppBarControlsDisabled(uiState.processing) }
  DisposableEffect(Unit) { onDispose { setAppBarControlsDisabled(false) } }

  LaunchedEffect(uiState.lastResponse, webViewRef) {
    val response = uiState.lastResponse ?: return@LaunchedEffect
    val webView = webViewRef ?: return@LaunchedEffect
    val escaped =
      response
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", "\\n")
    webView.evaluateJavascript("edgeCodex.showResponse('$escaped', 'assistant')", null)
  }

  LaunchedEffect(uiState.error, webViewRef) {
    val error = uiState.error ?: return@LaunchedEffect
    val webView = webViewRef ?: return@LaunchedEffect
    val escaped =
      error
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", "\\n")
    webView.evaluateJavascript("edgeCodex.showResponse('$escaped', 'system')", null)
  }

  if (!modelReady) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading Gemma 4 E2B for EdgeCodex...")
      }
    }
    return
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(bottom = bottomPadding)
  ) {
    if (uiState.processing) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        WebView(context).apply {
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true

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

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(EDGE_CODEX_ANDROID_BRIDGE_SCRIPT, null)
              }
            }

          webChromeClient =
            object : WebChromeClient() {
              override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                android.util.Log.d(
                  "EdgeCodex",
                  "${consoleMessage.message()} [${consoleMessage.lineNumber()}]",
                )
                return true
              }
            }

          addJavascriptInterface(
            EdgeCodexJsBridge(viewModel = viewModel, model = selectedModel),
            "Android",
          )
          loadUrl("https://appassets.androidplatform.net/assets/edgecodex/index.html")
          webViewRef = this
        }
      },
      update = { webView ->
        webView.removeJavascriptInterface("Android")
        webView.addJavascriptInterface(
          EdgeCodexJsBridge(viewModel = viewModel, model = selectedModel),
          "Android",
        )
      },
    )
  }
}

class EdgeCodexJsBridge(
  private val viewModel: EdgeCodexViewModel,
  private val model: Model,
) {
  @JavascriptInterface
  fun analyzeCode(userMessage: String, code: String) {
    viewModel.analyzeCode(model = model, userMessage = userMessage, code = code)
  }

  @JavascriptInterface
  fun setLanguage(language: String) {
    viewModel.setLanguage(model = model, language = language)
  }

  @JavascriptInterface
  fun updateConfig(configJson: String) {
    runCatching { Json.decodeFromString<CodexConfig>(configJson) }
      .onSuccess { config -> viewModel.updateConfig(model = model, newConfig = config) }
  }
}
