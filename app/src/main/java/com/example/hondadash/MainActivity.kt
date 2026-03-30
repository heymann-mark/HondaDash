package com.example.hondadash

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.webkit.JavascriptInterface
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private lateinit var dashWebView: WebView
    private lateinit var carWebView: WebView
    private lateinit var multiWebView: WebView
    private lateinit var mapsWebView: WebView
    private lateinit var batteryWebView: WebView
    private lateinit var tripsWebView: WebView
    private lateinit var tunerWebView: WebView
    private lateinit var navBar: LinearLayout
    private var currentScreen = "dash"

    private var carLoaded     = false
    private var multiLoaded   = false
    private var mapsLoaded    = false
    private var batteryLoaded = false
    private var tripsLoaded   = false
    private var tunerLoaded   = false

    private val NAV_HEIGHT = 140
    private val PERMISSION_REQUEST_CODE = 1001

    // Bluetooth
    private var bluetoothService: BluetoothService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bluetoothService = (binder as BluetoothService.LocalBinder).getService()
            bluetoothService?.listener = obdDataListener
            serviceBound = true
            Log.d("MainActivity", "BluetoothService bound")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
            serviceBound = false
        }
    }

    private val obdDataListener = object : BluetoothService.OBDDataListener {
        override fun onOBDData(data: OBDData) {
            // Dashboard gauges
            dashWebView.evaluateJavascript(
                "if(typeof updateGauges==='function')updateGauges(${data.rpm},${data.speed},${data.coolant},${data.throttle}," +
                "${data.o2Voltage},${data.iat},${data.timing},${data.stft},${data.ltft},${data.voltage},${data.idc})", null)

            // Battery page
            if (batteryLoaded) {
                batteryWebView.evaluateJavascript(
                    "if(typeof updateBatteryFromOBD==='function')updateBatteryFromOBD(${data.voltage})", null)
            }

            // Multi-system page — push individual ECM + BCM values
            if (multiLoaded) {
                multiWebView.evaluateJavascript(
                    "if(typeof updateFromOBD==='function'){" +
                    "updateFromOBD('ECM','rpm',${data.rpm});" +
                    "updateFromOBD('ECM','spd',${data.speed});" +
                    "updateFromOBD('ECM','ect',${data.coolant});" +
                    "updateFromOBD('ECM','tps',${data.throttle});" +
                    "updateFromOBD('ECM','iat',${data.iat});" +
                    "updateFromOBD('ECM','ign',${data.timing});" +
                    "updateFromOBD('ECM','stft',${data.stft});" +
                    "updateFromOBD('ECM','ltft',${data.ltft});" +
                    "updateFromOBD('ECM','afr',${data.o2Voltage});" +
                    "updateFromOBD('ECM','map',${data.map});" +
                    "updateFromOBD('ECM','inj',${data.idc});" +
                    "updateFromOBD('BCM','batt',${data.voltage});" +
                    "}", null)
            }

            // Tuner page
            if (tunerLoaded) {
                tunerWebView.evaluateJavascript(
                    "if(typeof updateFromFlashPro==='function')updateFromFlashPro({" +
                    "rpm:${data.rpm},vss:${data.speed},ect:${data.coolant},tps:${data.throttle}," +
                    "afr:${data.o2Voltage},iat:${data.iat},ign:${data.timing}," +
                    "stft:${data.stft},ltft:${data.ltft},batt:${data.voltage},inj:${data.idc}," +
                    "map:${data.map}})", null)
            }

            // Datalog (FlashPro format)
            dashWebView.evaluateJavascript(
                "if(typeof updateFromFlashPro==='function')updateFromFlashPro({" +
                "rpm:${data.rpm},vss:${data.speed},ect:${data.coolant},tps:${data.throttle}," +
                "afr:${data.o2Voltage},iat:${data.iat},ign:${data.timing}," +
                "stft:${data.stft},ltft:${data.ltft},batt:${data.voltage},inj:${data.idc}," +
                "map:${data.map}})", null)
        }

        override fun onDTCData(codes: List<String>) {
            if (carLoaded) {
                val jsonArray = codes.joinToString(",") { "\"$it\"" }
                carWebView.evaluateJavascript(
                    "if(typeof updateDTC==='function')updateDTC([$jsonArray])", null)
            }
        }

        override fun onConnectionStateChanged(state: BluetoothService.ConnectionState) {
            val stateStr = state.name
            Log.d("MainActivity", "BT state: $stateStr")

            // Update all loaded WebViews
            val js = "if(typeof updateConnectionStatus==='function')updateConnectionStatus('$stateStr')"
            dashWebView.evaluateJavascript(js, null)
            if (carLoaded) carWebView.evaluateJavascript(js, null)
            if (multiLoaded) multiWebView.evaluateJavascript(js, null)
            if (batteryLoaded) batteryWebView.evaluateJavascript(js, null)

            // Update BT nav button color
            updateBTButton(state)
        }
    }

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

        dashWebView = createAssetWebView()
        dashWebView.loadUrl("file:///android_asset/dashboard.html")
        root.addView(dashWebView, wvLP())

        carWebView = createCarWebView()
        carWebView.visibility = android.view.View.GONE
        root.addView(carWebView, wvLP())

        multiWebView = createAssetWebView()
        multiWebView.visibility = android.view.View.GONE
        root.addView(multiWebView, wvLP())

        mapsWebView = createInternetWebView()
        mapsWebView.visibility = android.view.View.GONE
        root.addView(mapsWebView, wvLP())

        batteryWebView = createAssetWebView()
        batteryWebView.visibility = android.view.View.GONE
        root.addView(batteryWebView, wvLP())

        tripsWebView = createInternetWebView()
        tripsWebView.visibility = android.view.View.GONE
        root.addView(tripsWebView, wvLP())

        tunerWebView = createAssetWebView()
        tunerWebView.addJavascriptInterface(CSVExporter(this), "Android")
        tunerWebView.visibility = android.view.View.GONE
        root.addView(tunerWebView, wvLP())

        addNavButton("⬡  GAUGES",   "dash",    true)
        addNavButton("◈  VEHICLE",  "car",     false)
        addNavButton("⬢  MULTI",    "multi",   false)
        addNavButton("⬡  MAPS",     "maps",    false)
        addNavButton("⚡  BATTERY", "battery", false)
        addNavButton("◈  TRIPS",    "trips",   false)
        addNavButton("⚙  TUNER",   "tuner",   false)
        addBTButton()

        setContentView(root)

        // Request permissions and bind Bluetooth service
        requestBluetoothPermissions()
    }

    // ============ BLUETOOTH PERMISSIONS ============

    private fun requestBluetoothPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (perms.isNotEmpty()) {
            requestPermissions(perms.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            bindBluetoothService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                bindBluetoothService()
            } else {
                Toast.makeText(this, "Bluetooth permissions needed for OBD connection", Toast.LENGTH_LONG).show()
                // Still bind — the service will just fail gracefully on connect
                bindBluetoothService()
            }
        }
    }

    private fun bindBluetoothService() {
        val intent = Intent(this, BluetoothService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ============ BT CONNECT BUTTON ============

    private var btButton: Button? = null

    private fun addBTButton() {
        val btn = Button(this)
        btn.text = "◉  BT"
        btn.textSize = 14f
        btn.setTypeface(null, Typeface.BOLD)
        btn.letterSpacing = 0.1f
        btn.setTextColor(Color.parseColor("#cc110066"))
        btn.setBackgroundColor(Color.TRANSPARENT)
        btn.setPadding(20, 0, 20, 0)
        btn.setOnClickListener { showDevicePicker() }
        btButton = btn
        navBar.addView(btn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.7f))
    }

    private fun updateBTButton(state: BluetoothService.ConnectionState) {
        btButton?.setTextColor(when (state) {
            BluetoothService.ConnectionState.CONNECTED -> Color.parseColor("#00ff41")
            BluetoothService.ConnectionState.CONNECTING,
            BluetoothService.ConnectionState.INITIALIZING -> Color.parseColor("#ffaa00")
            BluetoothService.ConnectionState.ERROR -> Color.parseColor("#cc2200")
            BluetoothService.ConnectionState.DISCONNECTED -> Color.parseColor("#cc110066")
        })
    }

    @SuppressLint("MissingPermission")
    private fun showDevicePicker() {
        val service = bluetoothService
        if (service == null) {
            Toast.makeText(this, "Bluetooth service not ready", Toast.LENGTH_SHORT).show()
            return
        }

        // If already connected, offer disconnect
        if (service.connectionState == BluetoothService.ConnectionState.CONNECTED) {
            service.disconnect()
            return
        }

        val devices = service.getPairedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found. Pair an OBD adapter in Settings first.", Toast.LENGTH_LONG).show()
            return
        }

        DevicePickerDialog.show(this, devices) { device ->
            service.connect(device)
        }
    }

    // ============ SCREEN NAVIGATION ============

    private fun switchTo(screen: String) {
        currentScreen = screen

        dashWebView.visibility    = if (screen == "dash")    android.view.View.VISIBLE else android.view.View.GONE
        carWebView.visibility     = if (screen == "car")     android.view.View.VISIBLE else android.view.View.GONE
        multiWebView.visibility   = if (screen == "multi")   android.view.View.VISIBLE else android.view.View.GONE
        mapsWebView.visibility    = if (screen == "maps")    android.view.View.VISIBLE else android.view.View.GONE
        batteryWebView.visibility = if (screen == "battery") android.view.View.VISIBLE else android.view.View.GONE
        tripsWebView.visibility   = if (screen == "trips")   android.view.View.VISIBLE else android.view.View.GONE
        tunerWebView.visibility   = if (screen == "tuner")   android.view.View.VISIBLE else android.view.View.GONE

        when (screen) {
            "car" -> if (!carLoaded) {
                val html = assets.open("carview.html").bufferedReader().readText()
                carWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                carLoaded = true
            }
            "multi" -> if (!multiLoaded) {
                multiWebView.loadUrl("file:///android_asset/multisystem.html")
                multiLoaded = true
            }
            "maps" -> if (!mapsLoaded) {
                val html = assets.open("streetview.html").bufferedReader().readText()
                mapsWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                mapsLoaded = true
            }
            "battery" -> if (!batteryLoaded) {
                batteryWebView.loadUrl("file:///android_asset/battery.html")
                batteryLoaded = true
            }
            "trips" -> if (!tripsLoaded) {
                val html = assets.open("trips.html").bufferedReader().readText()
                tripsWebView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
                tripsLoaded = true
            }
            "tuner" -> if (!tunerLoaded) {
                tunerWebView.loadUrl("file:///android_asset/tuner.html")
                tunerLoaded = true
            }
        }

        for (i in 0 until navBar.childCount) {
            val btn = navBar.getChildAt(i) as? Button ?: continue
            val tag = btn.tag as? String ?: continue
            btn.setTextColor(
                if (tag == screen) Color.parseColor("#cc2200ff")
                else Color.parseColor("#cc110066")
            )
        }
    }

    // ============ WEBVIEW FACTORIES ============

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
                Log.d("INTERCEPT", "url=$url fileName=$fileName")

                val isLocalAsset = fileName.endsWith(".glb") || fileName == "GLTFLoader.js"
                if (!isLocalAsset) return super.shouldInterceptRequest(view, request)

                return try {
                    Log.d("INTERCEPT", "Serving from assets: $fileName")
                    val stream: InputStream = assets.open(fileName)
                    val mime = when {
                        fileName.endsWith(".glb") -> "model/gltf-binary"
                        fileName.endsWith(".js")  -> "application/javascript"
                        else                      -> "application/octet-stream"
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
                    Log.e("INTERCEPT", "Failed to open asset: $fileName — ${e.message}")
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
                        else                       -> "application/octet-stream"
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

    // ============ SYSTEM UI ============

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

    // ============ LIFECYCLE ============

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onBackPressed() {
        val activeWv = when (currentScreen) {
            "car"     -> carWebView
            "multi"   -> multiWebView
            "maps"    -> mapsWebView
            "battery" -> batteryWebView
            "trips"   -> tripsWebView
            "tuner"   -> tunerWebView
            else      -> dashWebView
        }
        if (activeWv.canGoBack()) activeWv.goBack() else super.onBackPressed()
    }
}

class CSVExporter(private val context: Context) {

    @JavascriptInterface
    fun emailCSV(csvData: String, filename: String) {
        try {
            // Save CSV to cache dir
            val dir = File(context.cacheDir, "logs")
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(csvData)

            // Get content URI via FileProvider
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            // Send email directly
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("mark.heymann01@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "HondaDash Tuner Log — $filename")
                putExtra(Intent.EXTRA_TEXT, "Tuner datalog from HondaDash.\n\nK20Z3 // 2007 Civic Si FA5")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.gm")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Gmail not installed, fall back to chooser
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "Send tuner log"))
            }
        } catch (e: Exception) {
            Log.e("CSVExporter", "Failed to email CSV: ${e.message}")
        }
    }
}
