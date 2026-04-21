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
import android.widget.Toast
import android.graphics.Color
import android.net.Uri
import android.webkit.JavascriptInterface
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
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
            val js = """
                (function(){
                    var d={rpm:${data.rpm},vss:${data.speed},ect:${data.coolant},tps:${data.throttle},
                        afr:${data.o2Voltage},iat:${data.iat},ign:${data.timing},
                        stft:${data.stft},ltft:${data.ltft},batt:${data.voltage},inj:${data.idc},
                        map:${data.map},speed:${data.speed},coolant:${data.coolant},throttle:${data.throttle},
                        o2Voltage:${data.o2Voltage},timing:${data.timing}};
                    window._obdData=d;
                    var msg=JSON.stringify({type:'obd',data:d});
                    var frames=document.querySelectorAll('iframe');
                    for(var i=0;i<frames.length;i++){
                        try{ frames[i].contentWindow.postMessage(msg,'*'); }catch(e){}
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }

        override fun onDTCData(codes: List<String>) {
            val jsonArray = codes.joinToString(",") { "\"$it\"" }
            val js = """
                (function(){
                    var msg=JSON.stringify({type:'dtc',codes:[$jsonArray]});
                    var frames=document.querySelectorAll('iframe');
                    for(var i=0;i<frames.length;i++){
                        try{ frames[i].contentWindow.postMessage(msg,'*'); }catch(e){}
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }

        override fun onConnectionStateChanged(state: BluetoothService.ConnectionState) {
            val stateStr = state.name
            Log.d("MainActivity", "BT state: $stateStr")
            val js = """
                (function(){
                    if(typeof window.updateBTStatus==='function') window.updateBTStatus('$stateStr');
                    var msg=JSON.stringify({type:'btstate',state:'$stateStr'});
                    var frames=document.querySelectorAll('iframe');
                    for(var i=0;i<frames.length;i++){
                        try{ frames[i].contentWindow.postMessage(msg,'*'); }catch(e){}
                    }
                })()
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#0a0000"))

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        webView.settings.setGeolocationEnabled(true)
        webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        webView.settings.databaseEnabled = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(Color.parseColor("#0a0000"))

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
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
                        fileName.endsWith(".css")  -> "text/css"
                        fileName.endsWith(".png")  -> "image/png"
                        fileName.endsWith(".jpg")  -> "image/jpeg"
                        fileName.endsWith(".mp3")  -> "audio/mpeg"
                        fileName.endsWith(".svg")  -> "image/svg+xml"
                        else                       -> "application/octet-stream"
                    }
                    WebResourceResponse(mime, "UTF-8", 200, "OK",
                        mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "GET",
                            "Cache-Control" to "no-cache"
                        ),
                        stream)
                } catch (e: Exception) {
                    super.shouldInterceptRequest(view, request)
                }
            }
        }

        // Add JavaScript interfaces
        webView.addJavascriptInterface(CSVExporter(this), "Android")
        webView.addJavascriptInterface(BTInterface(), "AndroidBT")

        webView.loadUrl("file:///android_asset/index.html")

        val wvParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        root.addView(webView, wvParams)

        setContentView(root)
        requestBluetoothPermissions()
    }

    // ============ BLUETOOTH JS INTERFACE ============

    inner class BTInterface {
        @JavascriptInterface
        fun connect() {
            runOnUiThread { showDevicePicker() }
        }

        @JavascriptInterface
        fun disconnect() {
            bluetoothService?.disconnect()
        }

        @JavascriptInterface
        fun getState(): String {
            return bluetoothService?.connectionState?.name ?: "DISCONNECTED"
        }
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
                bindBluetoothService()
            }
        }
    }

    private fun bindBluetoothService() {
        val intent = Intent(this, BluetoothService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ============ BT DEVICE PICKER ============

    @SuppressLint("MissingPermission")
    private fun showDevicePicker() {
        val service = bluetoothService
        if (service == null) {
            Toast.makeText(this, "Bluetooth service not ready", Toast.LENGTH_SHORT).show()
            return
        }

        if (service.connectionState == BluetoothService.ConnectionState.CONNECTED) {
            service.disconnect()
            return
        }

        val devices = service.getPairedDevices()

        DevicePickerDialog.show(this, devices) { device ->
            Toast.makeText(this, "Connecting to ${device.name ?: device.address}...", Toast.LENGTH_SHORT).show()
            service.connect(device)
        }
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

    // ============ LIFECYCLE ============

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}

class CSVExporter(private val context: Context) {

    @JavascriptInterface
    fun emailCSV(csvData: String, filename: String) {
        try {
            val dir = File(context.cacheDir, "logs")
            dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(csvData)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

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
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "Send tuner log"))
            }
        } catch (e: Exception) {
            Log.e("CSVExporter", "Failed to email CSV: ${e.message}")
        }
    }
}
