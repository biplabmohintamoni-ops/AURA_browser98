package com.example

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val webViewMap = mutableMapOf<String, WebView>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val composeWebViewMap = remember { 
          mutableStateMapOf<String, WebView>().apply {
            putAll(webViewMap)
          } 
        }
        val viewModel: BrowserViewModel = viewModel()

        BrowserScreen(
          viewModel = viewModel,
          webViewMap = composeWebViewMap,
          modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(composeWebViewMap.size) {
          webViewMap.clear()
          webViewMap.putAll(composeWebViewMap)
        }
      }
    }
  }

  override fun onDestroy() {
    webViewMap.values.forEach { webView ->
      try {
        webView.stopLoading()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    webViewMap.clear()
    super.onDestroy()
  }
}

