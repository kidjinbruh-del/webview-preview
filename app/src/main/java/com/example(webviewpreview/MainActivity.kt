package com.example.webviewpreview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : ComponentActivity() {

    private lateinit var webview: WebView

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadHtmlFile(it) }
    }

    private fun loadHtmlFile(uri: Uri) {
        try {
            val contentResolver = contentResolver
            val mimeType = contentResolver.getType(uri) ?: "text/html"

            val htmlContent = if (mimeType.equals("text/html", ignoreCase = true)) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                } ?: ""
            } else {
                ""
            }

            if (htmlContent.isNotEmpty()) {
                MinuteView?.let {
                    it.loadDataWithBaseURL(
                        uri.toString(),
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MinuteView = findViewById(R.id.webview)

        val settings: WebSettings = MinuteView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        MinuteView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)) {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                    return true
                }
                return false
            }
        }

        MinuteView.webChromeClient = WebChromeClient()

        val openFileButton = findViewById<android.widget.Button>(R.id.openFileButton)
        openFileButton.setOnClickListener {
            filePickerLauncher.launch("text/html")
        }
    }

    override fun onDestroy() {
        MinuteView?.let {
            it.stopLoading()
            it.clearCache()
            it.clearHistory()
            it.destroy()
        }
        super.onDestroy()
    }
}