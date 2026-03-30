package com.example.hondadash

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val COMMAND_TIMEOUT_MS = 2500L
        private const val INIT_DELAY_MS = 300L
    }

    enum class ConnectionState { DISCONNECTED, CONNECTING, INITIALIZING, CONNECTED, ERROR }

    interface OBDDataListener {
        fun onOBDData(data: OBDData)
        fun onDTCData(codes: List<String>)
        fun onConnectionStateChanged(state: ConnectionState)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    private val binder = LocalBinder()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var pollThread: Thread? = null
    @Volatile private var isPolling = false

    var listener: OBDDataListener? = null
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    // Cached latest values — each poll cycle only queries a subset of PIDs
    @Volatile private var cachedData = OBDData()

    override fun onBind(intent: Intent?): IBinder = binder

    // ============ PUBLIC API ============

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
            emptyList()
        }
    }

    fun connect(device: BluetoothDevice) {
        // Run connection on background thread
        Thread {
            doConnect(device)
        }.apply {
            name = "BT-Connect"
            start()
        }
    }

    fun disconnect() {
        isPolling = false
        pollThread?.let { thread ->
            thread.interrupt()
            try { thread.join(1000) } catch (_: InterruptedException) {}
        }
        pollThread = null

        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null

        setState(ConnectionState.DISCONNECTED)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    // ============ CONNECTION ============

    @SuppressLint("MissingPermission")
    private fun doConnect(device: BluetoothDevice) {
        setState(ConnectionState.CONNECTING)

        try {
            // Cancel discovery to speed up connection
            try { adapter?.cancelDiscovery() } catch (_: SecurityException) {}

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket!!.connect()

            inputStream = socket!!.inputStream
            outputStream = socket!!.outputStream

            Log.i(TAG, "Socket connected to ${device.name}")
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed: ${e.message}")
            try { socket?.close() } catch (_: IOException) {}
            socket = null
            setState(ConnectionState.ERROR)
            return
        }

        // Initialize ELM327
        setState(ConnectionState.INITIALIZING)
        if (!initializeELM327()) {
            disconnect()
            setState(ConnectionState.ERROR)
            return
        }

        setState(ConnectionState.CONNECTED)
        startPolling()
    }

    private fun initializeELM327(): Boolean {
        val initCommands = listOf(
            "ATZ"  to 1500L,  // Reset — needs extra time
            "ATE0" to INIT_DELAY_MS,  // Echo off
            "ATL0" to INIT_DELAY_MS,  // Linefeeds off
            "ATS0" to INIT_DELAY_MS,  // Spaces off
            "ATH0" to INIT_DELAY_MS,  // Headers off
            "ATSP6" to INIT_DELAY_MS, // Protocol: ISO 15765-4 CAN (11-bit, 500kbps) — Honda Civic Si
        )

        for ((cmd, delay) in initCommands) {
            val response = sendCommand(cmd)
            Log.d(TAG, "Init: $cmd -> $response")
            if (OBD2Parser.isError(response) && cmd != "ATZ") {
                Log.e(TAG, "Init command failed: $cmd -> $response")
                // Don't fail on ATZ since it echoes version info
            }
            Thread.sleep(delay)
        }

        // Verify connection with voltage reading
        val voltResponse = sendCommand("ATRV")
        val voltage = OBD2Parser.parseVoltage(voltResponse)
        Log.i(TAG, "Init voltage: $voltage V (raw: $voltResponse)")
        return voltage != null && voltage > 6.0f
    }

    // ============ POLLING ============

    private fun startPolling() {
        isPolling = true
        pollThread = Thread {
            pollLoop()
        }.apply {
            name = "OBD-Poll"
            start()
        }
    }

    private fun pollLoop() {
        var cycle = 0

        while (isPolling && !Thread.currentThread().isInterrupted) {
            try {
                // Priority tier 1: Every cycle — RPM + Speed
                queryPID("010C") { raw -> OBD2Parser.parseRPM(raw)?.let { cachedData = cachedData.copy(rpm = it) } }
                queryPID("010D") { raw -> OBD2Parser.parseSpeed(raw)?.let { cachedData = cachedData.copy(speed = it) } }

                // Priority tier 2: Every 2nd cycle — Throttle + Coolant
                if (cycle % 2 == 0) {
                    queryPID("0111") { raw -> OBD2Parser.parseThrottle(raw)?.let { cachedData = cachedData.copy(throttle = it) } }
                    queryPID("0105") { raw -> OBD2Parser.parseCoolant(raw)?.let { cachedData = cachedData.copy(coolant = it) } }
                }

                // Priority tier 3: Every 4th cycle — IAT, Timing, STFT, O2
                if (cycle % 4 == 0) {
                    queryPID("010F") { raw -> OBD2Parser.parseIAT(raw)?.let { cachedData = cachedData.copy(iat = it) } }
                    queryPID("010E") { raw -> OBD2Parser.parseTiming(raw)?.let { cachedData = cachedData.copy(timing = it) } }
                    queryPID("0106") { raw -> OBD2Parser.parseSTFT(raw)?.let { cachedData = cachedData.copy(stft = it) } }
                    queryPID("0114") { raw -> OBD2Parser.parseO2Voltage(raw)?.let { cachedData = cachedData.copy(o2Voltage = it) } }
                }

                // Priority tier 4: Every 8th cycle — LTFT, MAP
                if (cycle % 8 == 0) {
                    queryPID("0107") { raw -> OBD2Parser.parseLTFT(raw)?.let { cachedData = cachedData.copy(ltft = it) } }
                    queryPID("010B") { raw -> OBD2Parser.parseMAP(raw)?.let { cachedData = cachedData.copy(map = it) } }
                }

                // Voltage: Every 30th cycle
                if (cycle % 30 == 0) {
                    val voltRaw = sendCommand("ATRV")
                    OBD2Parser.parseVoltage(voltRaw)?.let { cachedData = cachedData.copy(voltage = it) }
                }

                // DTCs: Every 200th cycle
                if (cycle % 200 == 0) {
                    val dtcRaw = sendCommand("03")
                    val codes = OBD2Parser.parseDTCs(dtcRaw)
                    if (codes.isNotEmpty()) {
                        val snapshot = codes.toList()
                        mainHandler.post { listener?.onDTCData(snapshot) }
                    }
                }

                // Post data snapshot to UI
                val snapshot = cachedData
                mainHandler.post { listener?.onOBDData(snapshot) }

                cycle++

            } catch (e: IOException) {
                Log.e(TAG, "Poll loop IO error: ${e.message}")
                isPolling = false
                mainHandler.post {
                    listener?.onConnectionStateChanged(ConnectionState.DISCONNECTED)
                }
                disconnect()
                return
            } catch (e: InterruptedException) {
                Log.d(TAG, "Poll loop interrupted")
                return
            }
        }
    }

    private inline fun queryPID(pid: String, parse: (String) -> Unit) {
        val raw = sendCommand(pid)
        if (!OBD2Parser.isError(raw)) {
            parse(raw)
        }
    }

    // ============ SERIAL I/O ============

    private fun sendCommand(cmd: String): String {
        val os = outputStream ?: throw IOException("Not connected")
        val ins = inputStream ?: throw IOException("Not connected")

        // Write command
        os.write("$cmd\r".toByteArray())
        os.flush()

        // Read until '>' prompt
        val buffer = StringBuilder()
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < COMMAND_TIMEOUT_MS) {
            if (ins.available() > 0) {
                val b = ins.read()
                if (b < 0) throw IOException("End of stream")
                val c = b.toChar()
                if (c == '>') break
                buffer.append(c)
            } else {
                Thread.sleep(5)
            }
        }

        return buffer.toString()
    }

    // ============ STATE ============

    private fun setState(state: ConnectionState) {
        connectionState = state
        Log.d(TAG, "State: $state")
        mainHandler.post { listener?.onConnectionStateChanged(state) }
    }
}
