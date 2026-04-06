package com.google.ai.edge.gallery.customtasks.edgecodex

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.webkit.WebViewAssetLoader

/**
 * Main Compose screen for EdgeCodex.
 *
 * The entire UI (editor + chat + prompt lab) is rendered in a WebView
 * for maximum flexibility and cross-platform consistency.
 *
 * Communication flow:
 *   User types question in WebView chat
 *   → JS calls Android.analyzeCode(question, code)
 *   → Kotlin sends to Gemma 4 E2B
 *   → Streaming response sent back via edgeCodex.showResponse()
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EdgeCodexScreen(
    viewModel: EdgeCodexViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Stream responses to WebView
    LaunchedEffect(uiState.streamingResponse) {
        val response = uiState.streamingResponse ?: return@LaunchedEffect
        val webView = webViewRef ?: return@LaunchedEffect
        val escaped = response
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
        webView.evaluateJavascript("edgeCodex.showResponse('$escaped', 'assistant')", null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading Gemma 4 E2B model...")
                    Text(
                        "First load may take a moment",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            return
        }

        uiState.error?.let { error ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
            return
        }

        // Full-screen WebView with the code editor + assistant UI
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    // JavaScript → Kotlin bridge
                    addJavascriptInterface(
                        EdgeCodexJsBridge(viewModel, this),
                        "Android"
                    )

                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler(
                            "/assets/",
                            WebViewAssetLoader.AssetsPathHandler(ctx)
                        )
                        .build()

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: android.webkit.WebResourceRequest
                        ) = assetLoader.shouldInterceptRequest(request.url)
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            android.util.Log.d("EdgeCodex", "${msg.message()} [${msg.lineNumber()}]")
                            return true
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/edgecodex/index.html")
                    webViewRef = this
                }
            },
        )
    }
}

/**
 * JavaScript interface exposed to WebView as `Android.*`.
 * WebView JS calls these methods to trigger Kotlin-side model inference.
 */
class EdgeCodexJsBridge(
    private val viewModel: EdgeCodexViewModel,
    private val webView: WebView,
) {
    @android.webkit.JavascriptInterface
    fun analyzeCode(userMessage: String, code: String) {
        viewModel.analyzeCode(userMessage, code)
    }

    @android.webkit.JavascriptInterface
    fun setLanguage(language: String) {
        viewModel.setLanguage(language)
    }

    @android.webkit.JavascriptInterface
    fun updateConfig(configJson: String) {
        try {
            val config = kotlinx.serialization.json.Json.decodeFromString<CodexConfig>(configJson)
            viewModel.updateConfig(config)
        } catch (_: Exception) { }
    }
}
