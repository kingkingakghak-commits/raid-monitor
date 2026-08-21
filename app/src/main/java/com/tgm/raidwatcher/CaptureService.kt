package com.tgm.raidwatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.ImageReader
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

        private const val CHANNEL_ID = "raid_monitor"
        private const val NOTIFICATION_ID = 1001
    }

    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
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
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            if (intent == null) {
                stopSelf()
                return START_NOT_STICKY
            }

            val resultCode =
                intent.getIntExtra(CODE, -1)

            val resultData =
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
                showMessage("❌ Screen capture permission missing")
                stopSelf()
                return START_NOT_STICKY
            }

            val manager =
                getSystemService(
                    MediaProjectionManager::class.java
                )

            projection =
                manager.getMediaProjection(
                    resultCode,
                    resultData
                )

            if (projection == null) {
                showMessage("❌ Could not start screen monitor")
                stopSelf()
                return START_NOT_STICKY
            }

            showMessage(
                "👀 TGM Raid Watcher\n" +
                "Monitoring is ON"
            )

        } catch (e: Exception) {

            showMessage(
                "❌ Monitor error\n${e.message ?: "Unknown error"}"
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
                "Raid monitoring is active"
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Raid Monitor",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "TGM Raid Watcher monitoring"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun showMessage(text: String) {

        if (!Settings.canDrawOverlays(this)) {
            return
        }

        overlay?.show(text)
    }

    override fun onDestroy() {

        try {
            imageReader?.close()
            imageReader = null

            projection?.stop()
            projection = null

            overlay?.hide()
            overlay = null

        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}


class RaidOverlay(
    private val context: Context
) {

    private var windowManager: WindowManager? = null
    private var textView: TextView? = null

    fun show(text: String) {

        if (!Settings.canDrawOverlays(context)) {
            return
        }

        if (textView == null) {

            windowManager =
                context.getSystemService(
                    WindowManager::class.java
                )

            textView =
                TextView(context).apply {

                    setTextColor(Color.WHITE)

                    setBackgroundColor(
                        Color.argb(
                            225,
                            20,
                            20,
                            20
                        )
                    )

                    setPadding(
                        20,
                        16,
                        20,
                        16
                    )

                    textSize = 16f

                    elevation = 10f
                }

            val type =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

            val params =
                WindowManager.LayoutParams(
                    420,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )

            params.gravity =
                Gravity.TOP or Gravity.END

            params.x = 12
            params.y = 120

            try {
                windowManager?.addView(
                    textView,
                    params
                )
            } catch (_: Exception) {
                textView = null
                return
            }
        }

        textView?.text = text
    }

    fun hide() {

        try {
            textView?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {
        }

        textView = null
    }
}
