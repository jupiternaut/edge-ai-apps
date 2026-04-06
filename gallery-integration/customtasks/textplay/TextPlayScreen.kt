package com.google.ai.edge.gallery.customtasks.textplay

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Main Compose screen for TextPlay game.
 *
 * Architecture:
 * - Top: WebView displaying the HTML5 game canvas
 * - Bottom: Text input for natural language commands
 * - WebView ←→ Kotlin communication via JavaScript evaluation
 *
 * Pattern follows TinyGardenScreen.kt from AI Edge Gallery.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TextPlayScreen(
    viewModel: TextPlayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    // Collect commands from ViewModel and execute in WebView
    LaunchedEffect(webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        viewModel.commandFlow.collect { command ->
            val json = Json.encodeToString(listOf(command))
            // Escape for JavaScript string
            val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript("textPlay.runCommands('$escaped')") { result ->
                // After command execution, sync game state back
                webView.evaluateJavascript("textPlay.getGameState()") { stateJson ->
                    stateJson?.let { viewModel.updateGameState(it) }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Loading / Error states
        if (uiState.loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading FunctionGemma model...")
                }
            }
            return
        }

        uiState.error?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
            return
        }

        // Game WebView
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false

                    // Load from local assets
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

                    // Debug logging
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            android.util.Log.d("TextPlay", "${msg.message()} [${msg.lineNumber()}]")
                            return true
                        }
                    }

                    loadUrl("https://appassets.androidplatform.net/assets/textplay/index.html")
                    webViewRef = this
                }
            },
        )

        // Processing indicator
        if (uiState.processing || uiState.resettingEngine) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Input bar
        Row(
            modifier = Modifier
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
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotEmpty()) {
                        inputText = ""
                        viewModel.processUserInput(text)
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
    }
}
