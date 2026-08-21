package com.tgm.raidwatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class CaptureService : Service() {

    companion object {
        const val CODE = "code"
        const val DATA = "data"

        private const val CHANNEL_ID = "raid_monitor_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var overlay: RaidOverlay? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        overlay = RaidOverlay(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        try {

            // Android requires the foreground notification first.
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            if (intent == null) {
                showOverlay("❌ Monitor data missing")
                stopSelf()
                return START_NOT_STICKY
            }

            val resultCode =
                intent.getIntExtra(CODE, -1)

            val resultData: Intent? =
                if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(
                        DATA,
                        Intent::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(DATA)
                }

            if (
                resultCode == -1 ||
                resultData == null
            ) {
                showOverlay(
                    "❌ Screen capture permission missing"
                )

                stopSelf()
                return START_NOT_STICKY
            }

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            mediaProjection =
                manager.getMediaProjection(
                    resultCode,
                    resultData
                )

            if (mediaProjection == null) {

                showOverlay(
                    "❌ Screen capture failed"
                )

                stopSelf()
                return START_NOT_STICKY
            }

            // For now this confirms that the capture service
            // and floating window are working.
            showOverlay(
                "🟢 TGM RAID WATCHER\n\n" +
                "Screen monitor: ON\n" +
                "Waiting for raid..."
            )

        } catch (e: Exception) {

            showOverlay(
                "❌ Monitor error\n\n" +
                (e.message ?: "Unknown error")
            )

            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
