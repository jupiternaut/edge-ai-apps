package com.google.ai.edge.gallery.customtasks.craftagents

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CraftAgentsScreen(
  modelManagerViewModel: ModelManagerViewModel,
  bottomPadding: Dp,
  setAppBarControlsDisabled: (Boolean) -> Unit,
  viewModel: CraftAgentsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
  val selectedModel = modelManagerUiState.selectedModel
  val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[selectedModel.name]
  val modelReady =
    selectedModel.instance != null &&
      modelInitializationStatus?.status != ModelInitializationStatusType.INITIALIZING

  val context = LocalContext.current
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  LaunchedEffect(uiState.isLoading) {
    setAppBarControlsDisabled(uiState.isLoading)
  }

  DisposableEffect(Unit) {
    onDispose {
      setAppBarControlsDisabled(false)
      webViewRef?.stopLoading()
    }
  }

  LaunchedEffect(uiState.connectedUrl, webViewRef) {
    val url = uiState.connectedUrl ?: return@LaunchedEffect
    val webView = webViewRef ?: return@LaunchedEffect
    if (webView.url != url) {
      webView.loadUrl(url)
    }
  }

  if (!modelReady) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Preparing Craft Agents remote client...")
      }
    }
    return
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
        .padding(bottom = bottomPadding),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Connect to a Craft Agents server with Web UI enabled.",
      style = MaterialTheme.typography.bodyMedium,
    )

    OutlinedTextField(
      value = uiState.draftServerUrl,
      onValueChange = viewModel::setDraftServerUrl,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("Craft Agents URL") },
      placeholder = { Text("https://your-craft-server.example.com") },
      singleLine = true,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(onClick = { viewModel.connect() }) {
        Text("Connect")
      }

      Button(
        onClick = {
          val url = uiState.connectedUrl ?: return@Button
          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        enabled = uiState.connectedUrl != null,
      ) {
        Text("Open External")
      }
    }

    uiState.pageTitle?.let { title ->
      Text(
        text = "Page: $title",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    uiState.error?.let { error ->
      Text(
        text = error,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }

    Box(modifier = Modifier.fillMaxSize()) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { androidContext ->
          WebView(androidContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            webChromeClient =
              object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                  super.onReceivedTitle(view, title)
                  viewModel.onPageFinished(title)
                }
              }

            webViewClient =
              object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                  super.onPageStarted(view, url, favicon)
                  viewModel.onPageStarted()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                  super.onPageFinished(view, url)
                  viewModel.onPageFinished(view?.title)
                }

                override fun onReceivedError(
                  view: WebView?,
                  request: WebResourceRequest?,
                  error: WebResourceError?,
                ) {
                  super.onReceivedError(view, request, error)
                  if (request?.isForMainFrame == true) {
                    viewModel.onPageError(error?.description?.toString() ?: "Failed to load Craft Agents page.")
                  }
                }

                override fun shouldOverrideUrlLoading(
                  view: WebView?,
                  request: WebResourceRequest?,
                ): Boolean {
                  val target = request?.url?.toString() ?: return false
                  if (target.startsWith("http://") || target.startsWith("https://")) {
                    return false
                  }

                  runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                  }
                  return true
                }
              }

            webViewRef = this
          }
        },
        update = { webView ->
          webViewRef = webView
        },
      )

      if (uiState.connectedUrl == null) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Enter your Craft Agents server URL and tap Connect.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (uiState.isLoading) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
    }
  }
}
