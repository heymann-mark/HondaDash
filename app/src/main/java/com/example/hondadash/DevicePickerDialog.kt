package com.example.hondadash

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

object DevicePickerDialog {

    private val BG_COLOR = Color.parseColor("#0d0000")
    private val ACCENT_COLOR = Color.parseColor("#cc2200")
    private val TEXT_COLOR = Color.parseColor("#ffffffcc")
    private val DIM_COLOR = Color.parseColor("#ffffff55")
    private val HINT_COLOR = Color.parseColor("#00cc88")
    private val DIVIDER_COLOR = Color.parseColor("#cc110033")

    private val OBD_KEYWORDS = listOf("obd", "elm", "flashpro", "vlink", "obdii", "scan")

    @SuppressLint("MissingPermission")
    fun show(activity: Activity, devices: List<BluetoothDevice>, onDeviceSelected: (BluetoothDevice) -> Unit) {
        // Sort: OBD-related devices first
        val sorted = devices.sortedByDescending { device ->
            val name = (device.name ?: "").lowercase()
            OBD_KEYWORDS.any { name.contains(it) }
        }

        val adapter = object : BaseAdapter() {
            override fun getCount() = sorted.size
            override fun getItem(pos: Int) = sorted[pos]
            override fun getItemId(pos: Int) = pos.toLong()

            @SuppressLint("MissingPermission")
            override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
                val device = sorted[pos]
                val name = device.name ?: "Unknown Device"
                val address = device.address
                val isOBD = OBD_KEYWORDS.any { name.lowercase().contains(it) }

                val layout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 32, 48, 32)
                    if (pos < sorted.size - 1) {
                        setBackgroundColor(Color.TRANSPARENT)
                    }
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
                }

                return layout
            }
        }

        // Title view
        val titleView = TextView(activity).apply {
            text = "SELECT OBD DEVICE"
            textSize = 14f
            setTextColor(ACCENT_COLOR)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 20)
        }

        val dialog = AlertDialog.Builder(activity)
            .setCustomTitle(titleView)
            .setAdapter(adapter) { _, which ->
                onDeviceSelected(sorted[which])
            }
            .setNegativeButton("CANCEL", null)
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
}
