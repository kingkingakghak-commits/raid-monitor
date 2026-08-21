package com.tgm.raidwatcher

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    companion object {
        private const val OVERLAY_REQUEST = 1001
        private const val CAPTURE_REQUEST = 1002
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

        if (!Settings.canDrawOverlays(this)) {

            statusText.text =
                "Allow display over other apps"

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

        requestScreenCapture()
    }

    private fun requestScreenCapture() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val intent =
            manager.createScreenCaptureIntent()

        startActivityForResult(
            intent,
            CAPTURE_REQUEST
        )
    }

    @Deprecated("Deprecated Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == OVERLAY_REQUEST) {

            if (Settings.canDrawOverlays(this)) {
                requestScreenCapture()
            } else {
                statusText.text =
                    "Overlay permission required"
            }

            return
        }

        if (requestCode == CAPTURE_REQUEST) {

            if (
                resultCode == RESULT_OK &&
                data != null
            ) {
                startCaptureService(
                    resultCode,
                    data
                )
            } else {
                statusText.text =
                    "Screen capture cancelled"
            }
        }
    }

    private fun startCaptureService(
        resultCode: Int,
        data: Intent
    ) {

        val intent =
            Intent(
                this,
                CaptureService::class.java
            ).apply {

                putExtra(
                    CaptureService.CODE,
                    resultCode
                )

                putExtra(
                    CaptureService.DATA,
                    data
                )
            }

        try {

            ContextCompat.startForegroundService(
                this,
                intent
            )

            statusText.text =
                "🟢 Raid Monitor started"

            startButton.isEnabled = false
            stopButton.isEnabled = true

        } catch (e: Exception) {

            statusText.text =
                "❌ Failed to start monitor"

            Toast.makeText(
                this,
                e.message ?: "Unknown error",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopMonitor() {

        stopService(
            Intent(
                this,
                CaptureService::class.java
            )
        )

        statusText.text =
            "🔴 Raid Monitor stopped"

        startButton.isEnabled = true
        stopButton.isEnabled = false
    }

    private fun updateStatus() {

        statusText.text =
            if (Settings.canDrawOverlays(this)) {
                "Ready to monitor incoming raids"
            } else {
                "Overlay permission required"
            }
    }

    override fun onResume() {
        super.onResume()

        if (::statusText.isInitialized) {
            updateStatus()
        }
    }
}
