package com.tgm.raidwatcher

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    private val requestCapture = 501
    private lateinit var status: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                status.text = "Grant overlay permission, then press Start again."
            } else {
                val mgr = getSystemService(MediaProjectionManager::class.java)
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (request != requestCapture || data == null) {
            status.text = "Status: screen capture cancelled."
            return
        }
        startForegroundService(Intent(this, CaptureService::class.java).apply {
            putExtra(CaptureService.CODE, result)
            putExtra(CaptureService.DATA, data)
        })
        status.text = "Status: monitoring."
    }
}
