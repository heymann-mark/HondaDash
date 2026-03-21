package com.example.hondadash

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Button
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import androidx.activity.ComponentActivity
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private lateinit var dashWebView: WebView
    private lateinit var carWebView: WebView
    private lateinit var engineWebView: WebView
    private lateinit var mapsWebView: WebView
    private lateinit var tripsWebView: WebView
    private lateinit var navBar: LinearLayout
    private var currentScreen = "dash"

    private var carLoaded    = false
    private var engineLoaded = false
    private var mapsLoaded   = false
    private var tripsLoaded  = false

    private val NAV_HEIGHT = 140

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0a0000"))

        navBar = LinearLayout(this)
        navBar.orientation = LinearLayout.HORIZONTAL
        navBar.setBackgroundColor(Color.parseColor("#0d0000"))
        navBar.gravity = Gravity.CENTER
        navBar.setPadding(0, 2, 0, 0)
        val navParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, NAV_HEIGHT)
        navParams.gravity = Gravity.BOTTOM
        root.addView(navBar, navParams)

        fun wvLP() = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).also { it.bottomMargin = NAV_HEIGHT }

        // GAUGES
        dashWebView = createAssetWebView()
        dashWebView.loadUrl("file:///android_asset/dashboard.html")
        root.addView(dashWebView, wvLP())

        // VEHICLE — full internet WebView with GLB asset interceptor
        carWebView = createCarWebView()
        carWebView.visibility = android.view.View.GONE
        root.addView(carWebView, wvLP())

        // ENGINE
        engineWebView = createAssetWebView()
        engineWebView.visibility = android.view.View.GONE
        root.addView(engineWebView, wvLP())

        // MAPS
        mapsWebView = createInternetWebView()
        mapsWebView.visibility = android.view.View.GONE
        root.addView(mapsWebView, wvLP())

        // TRIPS
        tripsWebView = createInternetWebView()
        tripsWebView.visibility = android.view.View.GONE
        root.addView(tripsWebView, wvLP())

        addNavButton("⬡  GAUGES",  "dash",   true)
        addNavButton("◈  VEHICLE", "car",    false)
        addNavButton("⬢  ENGINE",  "engine", false)
        addNavButton("⬡  MAPS",    "maps",   false)
        addNavButton("◈  TRIPS",   "trips",  false)

        setContentView(root)
    }

    private fun switchTo(screen: String) {
        currentScreen = screen

        dashWebView.visibility   = if (screen == "dash")   android.view.View.VISIBLE else android.view.View.GONE
        carWebView.visibility    = if (screen == "car")    android.view.View.VISIBLE else android.view.View.GONE
        engineWebView.visibility = if (screen == "engine") android.view.View.VISIBLE else android.view.View.GONE
        mapsWebView.visibility   = if (screen == "maps")   android.view.View.VISIBLE else android.view.View.GONE
        tripsWebView.visibility  = if (screen == "trips")  android.view.View.VISIBLE else android.view.View.GONE

        when (screen) {
            "car" -> if (!carLoaded) {
                val html = assets.open("streetview.html").bufferedReader().readText()
                carWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                carLoaded = true
            }
            "engine" -> if (!engineLoaded) {
                engineWebView.loadUrl("file:///android_asset/engine3d.html")
                engineLoaded = true
            }
            "maps" -> if (!mapsLoaded) {
                mapsWebView.loadUrl("file:///android_asset/maps.html")
                mapsLoaded = true
            }
            "trips" -> if (!tripsLoaded) {
                val html = assets.open("trips.html").bufferedReader().readText()
                tripsWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                tripsLoaded = true
            }
        }

        for (i in 0 until navBar.childCount) {
            val btn = navBar.getChildAt(i) as? Button ?: continue
            btn.setTextColor(
                if (btn.tag == screen) Color.parseColor("#cc2200ff")
                else Color.parseColor("#cc110066")
            )
        }
    }

    // VEHICLE WebView — identical to internet WebView but intercepts .glb and GLTFLoader.js from assets.
    // Everything else (Mapbox tiles, CDN scripts) hits the internet normally.
    @SuppressLint("SetJavaScriptEnabled")
    private fun createCarWebView(): WebView {
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.allowFileAccess = true
        wv.settings.allowFileAccessFromFileURLs = true
        wv.settings.allowUniversalAccessFromFileURLs = true
        wv.settings.setGeolocationEnabled(true)
        wv.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        wv.settings.userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        wv.settings.databaseEnabled = true
        wv.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        wv.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }
        wv.setBackgroundColor(Color.parseColor("#0a0000"))
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                val fileName = url.substringAfterLast("/").substringBefore("?")
                val isLocalAsset = fileName.endsWith(".glb") || fileName == "GLTFLoader.js"
                if (!isLocalAsset) return super.shouldInterceptRequest(view, request)
                return try {
                    val stream: InputStream = assets.open(fileName)
                    val mime = when {
                        fileName.endsWith(".glb") -> "model/gltf-binary"
                        fileName.endsWith(".js")  -> "application/javascript"
                        else -> "application/octet-stream"
                    }
                    WebResourceResponse(mime, "UTF-8", 200, "OK",
                        mapOf(
                            "Access-Control-Allow-Origin"  to "*",
                            "Access-Control-Allow-Methods" to "GET",
                            "Cache-Control"                to "no-cache"
                        ),
                        stream
                    )
                } catch (e: Exception) {
                    super.shouldInterceptRequest(view, request)
                }
            }
        }
        return wv
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createAssetWebView(): WebView {
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.allowFileAccess = true
        wv.settings.allowFileAccessFromFileURLs = true
        wv.settings.allowUniversalAccessFromFileURLs = true
        wv.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        wv.settings.userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        wv.webChromeClient = WebChromeClient()
        wv.setBackgroundColor(Color.parseColor("#0a0000"))
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (!url.startsWith("file:///") && !url.startsWith("https://localhost")) {
                    return super.shouldInterceptRequest(view, request)
                }
                val fileName = url.substringAfterLast("/").substringBefore("?")
                return try {
                    val stream: InputStream = assets.open(fileName)
                    val mime = when {
                        fileName.endsWith(".glb")  -> "model/gltf-binary"
                        fileName.endsWith(".html") -> "text/html"
                        fileName.endsWith(".js")   -> "application/javascript"
                        fileName.endsWith(".png")  -> "image/png"
                        fileName.endsWith(".jpg")  -> "image/jpeg"
                        fileName.endsWith(".mp3")  -> "audio/mpeg"
                        else -> "application/octet-stream"
                    }
                    WebResourceResponse(mime, "UTF-8", 200, "OK",
                        mapOf("Access-Control-Allow-Origin" to "*", "Cache-Control" to "no-cache"),
                        stream)
                } catch (e: Exception) {
                    super.shouldInterceptRequest(view, request)
                }
            }
        }
        return wv
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createInternetWebView(): WebView {
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.allowFileAccess = true
        wv.settings.allowFileAccessFromFileURLs = true
        wv.settings.allowUniversalAccessFromFileURLs = true
        wv.settings.setGeolocationEnabled(true)
        wv.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        wv.settings.userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        wv.settings.databaseEnabled = true
        wv.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        wv.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }
        wv.webViewClient = WebViewClient()
        wv.setBackgroundColor(Color.parseColor("#0a0000"))
        return wv
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun addNavButton(label: String, screen: String, isActive: Boolean) {
        val btn = Button(this)
        btn.text = label
        btn.textSize = 16f
        btn.setTypeface(null, Typeface.BOLD)
        btn.letterSpacing = 0.15f
        btn.setTextColor(if (isActive) Color.parseColor("#cc2200ff") else Color.parseColor("#cc110066"))
        btn.setBackgroundColor(Color.TRANSPARENT)
        btn.setPadding(20, 0, 20, 0)
        btn.tag = screen
        btn.setOnClickListener { switchTo(screen) }
        navBar.addView(btn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
    }

    override fun onBackPressed() {
        val activeWv = when (currentScreen) {
            "car"    -> carWebView
            "engine" -> engineWebView
            "maps"   -> mapsWebView
            "trips"  -> tripsWebView
            else     -> dashWebView
        }
        if (activeWv.canGoBack()) activeWv.goBack() else super.onBackPressed()
    }
}
