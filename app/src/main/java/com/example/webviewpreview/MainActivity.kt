package com.example.webviewpreview

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var webview: WebView
    private var currentBaseUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { loadHtmlFile(it) }
    }

    private val dirPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { loadHtmlFromDirectory(it) }
    }

    private fun loadHtmlFile(uri: Uri) {
        try {
            val contentResolver = contentResolver
            val mimeType = contentResolver.getType(uri) ?: "text/html"
            val htmlContent = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { reader -> reader.readText() }
            } ?: ""
            if (htmlContent.isNotEmpty()) {
                currentBaseUri = uri
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
                    currentBaseUri = htmlFile.uri
                    webview.loadDataWithBaseURL(dirUri.toString(), htmlContent, "text/html", "UTF-8", null)
                }
            } else {
                Toast.makeText(this, "index.html не найден в папке", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
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

        openFileButton.setOnClickListener {
            filePickerLauncher.launch("text/html")
        }
        openDirButton.setOnClickListener {
            dirPickerLauncher.launch(null)
        }
        reloadButton.setOnClickListener {
            webview.reload()
        }

        // Drag-and-Drop для WebView
        webview.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTER -> {
                    v.alpha = 0.7f
                    true
                }
                DragEvent.ACTION_DRAG_EXIT -> {
                    v.alpha = 1.0f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1.0f
                    val clipData = event.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val item = clipData.getItemAt(0)
                        val uri = item.uri
                        loadHtmlFile(uri)
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        // Long-press для начала drag
        webview.setOnLongClickListener { v ->
            val clipData = ClipData.newPlainText("text", "drag")
            val shadow = android.view.View.DragShadowBuilder(v)
            v.startDragAndDrop(clipData, shadow, v, 0)
            true
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