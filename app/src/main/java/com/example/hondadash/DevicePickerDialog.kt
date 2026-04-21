package com.example.hondadash

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

object DevicePickerDialog {

    private val BG_COLOR = Color.parseColor("#0d0000")
    private val ACCENT_COLOR = Color.parseColor("#cc2200")
    private val TEXT_COLOR = Color.parseColor("#ffffffcc")
    private val DIM_COLOR = Color.parseColor("#ffffff55")
    private val HINT_COLOR = Color.parseColor("#00cc88")
    private val SCAN_COLOR = Color.parseColor("#ffaa00")
    private val DIVIDER_COLOR = Color.parseColor("#cc110033")

    private val OBD_KEYWORDS = listOf("obd", "elm", "flashpro", "vlink", "v-link", "obdii", "scan", "hondata")

    @SuppressLint("MissingPermission")
    fun show(activity: Activity, pairedDevices: List<BluetoothDevice>, onDeviceSelected: (BluetoothDevice) -> Unit) {
        val allDevices = mutableListOf<BluetoothDevice>()
        val discoveredAddresses = mutableSetOf<String>()

        // Add paired devices first
        allDevices.addAll(pairedDevices)
        pairedDevices.forEach { discoveredAddresses.add(it.address) }

        val adapter = DeviceListAdapter(activity, allDevices, pairedDevices.map { it.address }.toSet())

        // Title view
        val titleLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 40, 0, 10)
        }
        val titleView = TextView(activity).apply {
            text = "SELECT OBD DEVICE"
            textSize = 14f
            setTextColor(ACCENT_COLOR)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
        }
        val scanStatus = TextView(activity).apply {
            text = "SCANNING FOR DEVICES..."
            textSize = 10f
            setTextColor(SCAN_COLOR)
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }
        titleLayout.addView(titleView)
        titleLayout.addView(scanStatus)

        // BroadcastReceiver for discovered devices
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && device.address !in discoveredAddresses) {
                            discoveredAddresses.add(device.address)
                            allDevices.add(device)
                            // Re-sort: OBD devices first, then paired, then discovered
                            allDevices.sortWith(compareByDescending<BluetoothDevice> { d ->
                                val name = (d.name ?: "").lowercase()
                                OBD_KEYWORDS.any { name.contains(it) }
                            }.thenByDescending { d ->
                                d.bondState == BluetoothDevice.BOND_BONDED
                            })
                            adapter.notifyDataSetChanged()
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanStatus.text = "SCAN COMPLETE"
                        scanStatus.setTextColor(DIM_COLOR)
                    }
                }
            }
        }

        // Register receiver and start discovery
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        activity.registerReceiver(receiver, filter)

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        btAdapter?.cancelDiscovery()
        btAdapter?.startDiscovery()

        val dialog = AlertDialog.Builder(activity)
            .setCustomTitle(titleLayout)
            .setAdapter(adapter) { _, which ->
                btAdapter?.cancelDiscovery()
                try { activity.unregisterReceiver(receiver) } catch (_: Exception) {}
                val device = allDevices[which]
                val dName = device.name ?: device.address
                // If not yet paired, initiate bonding first
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    android.widget.Toast.makeText(activity, "Pairing with $dName...", android.widget.Toast.LENGTH_SHORT).show()
                    device.createBond()
                    // Wait for bond then connect
                    val bondReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                                if (state == BluetoothDevice.BOND_BONDED) {
                                    android.widget.Toast.makeText(activity, "Paired! Connecting to $dName...", android.widget.Toast.LENGTH_SHORT).show()
                                    try { activity.unregisterReceiver(this) } catch (_: Exception) {}
                                    onDeviceSelected(device)
                                } else if (state == BluetoothDevice.BOND_NONE) {
                                    android.widget.Toast.makeText(activity, "Pairing failed with $dName", android.widget.Toast.LENGTH_SHORT).show()
                                    try { activity.unregisterReceiver(this) } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                    activity.registerReceiver(bondReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                } else {
                    onDeviceSelected(device)
                }
            }
            .setNegativeButton("CANCEL", null)
            .setOnDismissListener {
                btAdapter?.cancelDiscovery()
                try { activity.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
            .create()

        dialog.setOnShowListener {
            dialog.window?.decorView?.setBackgroundColor(BG_COLOR)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(DIM_COLOR)
                typeface = Typeface.create("monospace", Typeface.NORMAL)
                letterSpacing = 0.15f
            }
            dialog.listView?.apply {
                setBackgroundColor(BG_COLOR)
                divider = android.graphics.drawable.ColorDrawable(DIVIDER_COLOR)
                dividerHeight = 1
            }
        }

        dialog.show()
    }

    @SuppressLint("MissingPermission")
    private class DeviceListAdapter(
        private val activity: Activity,
        private val devices: MutableList<BluetoothDevice>,
        private val pairedAddresses: Set<String>
    ) : BaseAdapter() {
        override fun getCount() = devices.size
        override fun getItem(pos: Int) = devices[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
            val device = devices[pos]
            val name = device.name ?: "Unknown Device"
            val address = device.address
            val isOBD = OBD_KEYWORDS.any { name.lowercase().contains(it) }
            val isPaired = address in pairedAddresses

            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 32)
            }

            val nameView = TextView(activity).apply {
                text = name
                textSize = 15f
                setTextColor(if (isOBD) HINT_COLOR else TEXT_COLOR)
                typeface = Typeface.create("monospace", Typeface.BOLD)
                letterSpacing = 0.05f
            }

            val addrView = TextView(activity).apply {
                text = address
                textSize = 11f
                setTextColor(DIM_COLOR)
                typeface = Typeface.create("monospace", Typeface.NORMAL)
                letterSpacing = 0.1f
            }

            layout.addView(nameView)
            layout.addView(addrView)

            if (isOBD) {
                val badge = TextView(activity).apply {
                    text = "OBD ADAPTER"
                    textSize = 9f
                    setTextColor(HINT_COLOR)
                    typeface = Typeface.create("monospace", Typeface.BOLD)
                    letterSpacing = 0.2f
                    setPadding(0, 8, 0, 0)
                }
                layout.addView(badge)
            } else if (!isPaired) {
                val badge = TextView(activity).apply {
                    text = "NEW DEVICE"
                    textSize = 9f
                    setTextColor(SCAN_COLOR)
                    typeface = Typeface.create("monospace", Typeface.BOLD)
                    letterSpacing = 0.2f
                    setPadding(0, 8, 0, 0)
                }
                layout.addView(badge)
            }

            return layout
        }
    }
}
