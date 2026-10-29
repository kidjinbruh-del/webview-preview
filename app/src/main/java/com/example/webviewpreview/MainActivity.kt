package com.example.webviewpreview

import android.content.ClipData
import android.content.Intent
import android.net.Uri
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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Collections

class MainActivity : ComponentActivity() {

    private lateinit var webview: WebView
    private val recentFiles = Collections.synchronizedList(mutableListOf<Uri>())
    private var isDesktopMode by mutableStateOf(false)
    private var isDarkTheme by mutableStateOf(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            loadHtmlFile(it)
            addToRecent(it)
        }
    }

    private val dirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            loadHtmlFromDirectory(it)
            addToRecent(it)
        }
    }

    private fun loadHtmlFile(uri: Uri) {
        try {
            val contentResolver = contentResolver
            val mimeType = contentResolver.getType(uri) ?: "text/html"
            val htmlContent = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { reader -> reader.readText() }
            } ?: ""
            if (htmlContent.isNotEmpty()) {
                webview.loadDataWithBaseURL(uri.toString(), htmlContent, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadHtmlFromDirectory(dirUri: Uri) {
        try {
            val contentResolver = contentResolver
            val docFile = android.content.DocumentFile.fromTreeUri(this, dirUri)
            val htmlFile = docFile?.findFile("index.html")
            if (htmlFile != null && htmlFile.exists()) {
                val htmlContent = contentResolver.openInputStream(htmlFile.uri)?.use { stream ->
                    stream.bufferedReader().use { reader -> reader.readText() }
                } ?: ""
                if (htmlContent.isNotEmpty()) {
                    webview.loadDataWithBaseURL(dirUri.toString(), htmlContent, "text/html", "UTF-8", null)
                }
            } else {
                Toast.makeText(this, "index.html не найден в папке", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addToRecent(uri: Uri) {
        recentFiles.remove(uri)
        recentFiles.add(0, uri)
        if (recentFiles.size > 10) {
            recentFiles.removeAt(recentFiles.size - 1)
        }
    }

    private fun applyViewportSettings() {
        val settings = webview.settings
        if (isDesktopMode) {
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            webview.post { webview.zoomBy(1.2f) }
        } else {
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webview.post { webview.zoomBy(0.8f) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webview = findViewById(R.id.webview)

        val settings: WebSettings = webview.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return false
            }
        }
        webview.webChromeClient = WebChromeClient()

        val openFileButton = findViewById<android.widget.Button>(R.id.openFileButton)
        val openDirButton = findViewById<android.widget.Button>(R.id.openDirButton)
        val reloadButton = findViewById<android.widget.Button>(R.id.reloadButton)
        val desktopButton = findViewById<android.widget.Button>(R.id.desktopButton)
        val themeButton = findViewById<android.widget.Button>(R.id.themeButton)
        val recentButton = findViewById<android.widget.Button>(R.id.recentButton)

        openFileButton.setOnClickListener { filePickerLauncher.launch("text/html") }
        openDirButton.setOnClickListener { dirPickerLauncher.launch(null) }
        reloadButton.setOnClickListener { webview.reload() }

        desktopButton.setOnClickListener {
            isDesktopMode = !isDesktopMode
            applyViewportSettings()
            desktopButton.text = if (isDesktopMode) "Мобильно" else "Десктоп"
        }

        themeButton.setOnClickListener {
            isDarkTheme = !isDarkTheme
            androidx.core.content.res.ConfigurationCompat
                .getResources(configuration)
            recreate()
        }

        recentButton.setOnClickListener {
            if (recentFiles.isNotEmpty()) {
                loadHtmlFile(recentFiles[0])
            } else {
                Toast.makeText(this, "Нет недавних файлов", Toast.LENGTH_SHORT).show()
            }
        }

        webview.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTER -> { v.alpha = 0.7f; true }
                DragEvent.ACTION_DRAG_EXIT -> { v.alpha = 1.0f; true }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1.0f
                    val clipData = event.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val uri = clipData.getItemAt(0).uri
                        loadHtmlFile(uri)
                        addToRecent(uri)
                        true
                    } else false
                }
                else -> false
            }
        }
        webview.setOnLongClickListener { v ->
            val clipData = ClipData.newPlainText("text", "drag")
            val shadow = android.view.View.DragShadowBuilder(v)
            v.startDragAndDrop(clipData, shadow, v, 0)
            true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::webview.isInitialized) {
            applyViewportSettings()
        }
    }

    override fun onDestroy() {
        webview.stopLoading()
        webview.clearCache()
        webview.clearHistory()
        webview.destroy()
        super.onDestroy()
    }
}