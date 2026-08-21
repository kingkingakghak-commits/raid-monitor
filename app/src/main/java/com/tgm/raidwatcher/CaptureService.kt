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
    private var raidOverlay: RaidOverlay? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        raidOverlay = RaidOverlay(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        try {
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            if (intent == null) {
                showOverlay("Monitor data missing")
                stopSelf()
                return START_NOT_STICKY
            }

            val resultCode = intent.getIntExtra(
                CODE,
                -1
            )

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

            if (resultCode == -1 || resultData == null) {
                showOverlay(
                    "Screen capture permission missing"
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
                    "Screen capture failed"
                )
                stopSelf()
                return START_NOT_STICKY
            }

            showOverlay(
                "TGM RAID WATCHER\n\n" +
                    "Screen monitor: ON\n" +
                    "Waiting for raid..."
            )

        } catch (e: Exception) {

            showOverlay(
                "Monitor error\n\n" +
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
        )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .setContentTitle(
                "TGM Raid Watcher"
            )
            .setContentText(
                "Raid monitor is running"
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "TGM Raid Watcher",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "TGM Raid Watcher screen monitoring"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun showOverlay(message: String) {

        if (!Settings.canDrawOverlays(this)) {
            return
        }

        raidOverlay?.show(message)
    }

    override fun onDestroy() {

        try {
            raidOverlay?.hide()
        } catch (_: Exception) {
        }

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }

        raidOverlay = null
        mediaProjection = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

private class RaidOverlay(
    private val context: Context
) {

    private var windowManager: WindowManager? = null
    private var textView: TextView? = null

    fun show(message: String) {

        if (!Settings.canDrawOverlays(context)) {
            return
        }

        if (textView == null) {

            windowManager =
                context.getSystemService(
                    Context.WINDOW_SERVICE
                ) as WindowManager

            textView = TextView(context).apply {
                setTextColor(Color.WHITE)

                setBackgroundColor(
                    Color.rgb(25, 25, 25)
                )

                setPadding(
                    24,
                    18,
                    24,
                    18
                )

                textSize = 16f
            }

            val windowType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity =
                Gravity.TOP or Gravity.CENTER_HORIZONTAL

            params.x = 0
            params.y = 120

            try {
                windowManager?.addView(
                    textView,
                    params
                )
            } catch (_: Exception) {
                textView = null
                windowManager = null
                return
            }
        }

        textView?.text = message
    }

    fun hide() {

        try {
            textView?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {
        }

        textView = null
        windowManager = null
    }
}
