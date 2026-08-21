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

        if (!Settings.canDrawOverlays(this)) {

            Toast.makeText(
                this,
                "Please allow display over other apps",
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
                "Screen capture could not start"

            Toast.makeText(
                this,
                "Screen capture error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Deprecated in Android API")
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

        when (requestCode) {

            OVERLAY_REQUEST -> {

                if (Settings.canDrawOverlays(this)) {
                    requestScreenCapture()
                } else {
                    statusText.text =
                        "Floating-window permission is required"
                }
            }

            SCREEN_CAPTURE_REQUEST -> {

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
                        "Screen capture permission cancelled"
                }
            }
        }
    }

    private fun startCaptureService(
        resultCode: Int,
        data: Intent
    ) {

        try {

            val serviceIntent =
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                    this,
                    serviceIntent
                )

            } else {

                startService(serviceIntent)
            }

            statusText.text =
                "Raid Monitor is running"

            startButton.isEnabled = false
            stopButton.isEnabled = true

        } catch (e: Exception) {

            statusText.text =
                "Failed to start monitor"

            Toast.makeText(
                this,
                "Monitor error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopMonitor() {

        try {

            stopService(
                Intent(
                    this,
                    CaptureService::class.java
                )
            )

        } catch (_: Exception) {
        }

        statusText.text =
            "Raid Monitor stopped"

        startButton.isEnabled = true
        stopButton.isEnabled = false
    }

    private fun updateStatus() {

        statusText.text =
            if (Settings.canDrawOverlays(this)) {
                "Ready to monitor incoming raids"
            } else {
                "Allow floating-window permission first"
            }
    }

    override fun onResume() {
        super.onResume()

        if (::statusText.isInitialized) {
            updateStatus()
        }
    }
}
