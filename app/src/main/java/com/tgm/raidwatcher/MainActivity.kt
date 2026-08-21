package com.tgm.raidwatcher

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    companion object {
        private const val SCREEN_CAPTURE_REQUEST = 1001
        private const val OVERLAY_REQUEST = 1002
    }

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        stopButton.isEnabled = false

        startButton.setOnClickListener {
            startMonitor()
        }

        stopButton.setOnClickListener {
            stopMonitor()
        }

        updateStatus()
    }

    private fun startMonitor() {

        // Step 1: request floating-window permission
        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                this,
                "Allow TGM Raid Watcher to display over other apps",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )

            startActivityForResult(
                intent,
                OVERLAY_REQUEST
            )

            return
        }

        // Step 2: request screen capture permission
        requestScreenCapture()
    }

    private fun requestScreenCapture() {

        try {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            val captureIntent =
                manager.createScreenCaptureIntent()

            startActivityForResult(
                captureIntent,
                SCREEN_CAPTURE_REQUEST
            )

        } catch (e: Exception) {

            statusText.text =
                "❌ Could not request screen capture\n${e.message}"

            Toast.makeText(
                this,
                "Screen capture
