package com.tgm.raidwatcher

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.*
import android.media.*
import android.media.projection.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

class CaptureService : Service() {
    companion object { const val CODE="code"; const val DATA="data" }
    private var projection:MediaProjection?=null
    private var display:VirtualDisplay?=null
    private var reader:ImageReader?=null
    private val exec=Executors.newSingleThreadExecutor()
    private var last=0L
    private var fingerprint=""
    private lateinit var overlay:RaidOverlay

    override fun onCreate(){
        super.onCreate()
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("raid","Raid monitor",NotificationManager.IMPORTANCE_LOW))
        overlay=RaidOverlay(this)
        overlay.show("👀 Raid Watcher\nWaiting for an incoming raid…")
    }

    override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int{
        startForeground(77,NotificationCompat.Builder(this,"raid")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("TGM Raid Watcher")
            .setContentText("Monitoring for incoming raids")
            .setOngoing(true).build())
        val code=i?.getIntExtra(CODE,-1) ?: return START_NOT_STICKY
        val data=i.getParcelableExtra<Intent>(DATA) ?: return START_NOT_STICKY
        projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(code,data)
        capture()
        return START_NOT_STICKY
    }

    private fun capture(){
        val m=resources.displayMetrics
        reader=ImageReader.newInstance(m.widthPixels,m.heightPixels,PixelFormat.RGBA_8888,2)
        reader!!.setOnImageAvailableListener({r->
            val now=SystemClock.elapsedRealtime()
            if(now-last<900){r.acquireLatestImage()?.close();return@setOnImageAvailableListener}
            last=now
            val image=r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try{
                val b=Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888)
                b.copyPixelsFromBuffer(image.planes[0].buffer)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(InputImage.fromBitmap(b,0))
                    .addOnSuccessListener(exec){ result->
                        val p=RaidParser.parse(result.text)
                        if(p.found && p.fingerprint!=fingerprint){fingerprint=p.fingerprint;overlay.show(p.display())}
                        else if(!p.found && fingerprint.isNotEmpty()){fingerprint="";overlay.show("👀 Raid Watcher\nWaiting for an incoming raid…")}
                    }
            }finally{image.close()}
        },Handler(Looper.getMainLooper()))
        display=projection?.createVirtualDisplay("TGM Raid Watcher",m.widthPixels,m.heightPixels,m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null)
    }

    override fun onDestroy(){
        display?.release();reader?.close();projection?.stop();exec.shutdownNow()
        if(::overlay.isInitialized)overlay.hide()
        super.onDestroy()
    }
    override fun onBind(i:Intent?):IBinder?=null
}

class RaidOverlay(private val c:Context){
    private var wm:WindowManager?=null
    private var v:TextView?=null
    fun show(s:String){
        if(!Settings.canDrawOverlays(c))return
        if(v==null){
            wm=c.getSystemService(WindowManager::class.java)
            v=TextView(c).apply{setTextColor(Color.WHITE);setBackgroundColor(0xDD111111.toInt());setPadding(20,16,20,16);textSize=15f}
            val p=WindowManager.LayoutParams(400,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
            p.gravity=Gravity.TOP or Gravity.END;p.x=12;p.y=120
            wm?.addView(v,p)
        }
        v?.text=s
    }
    fun hide(){v?.let{runCatching{wm?.removeView(it)}};v=null}
}
