package com.esona.webcamcloud.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.ui.MainActivity
import com.esona.webcamcloud.util.Utils
import com.esona.webcamcloud.util.WifiMonitor
import com.pedro.rtplibrary.rtsp.RtspCamera1
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit

class CamService : Service(), ConnectCheckerRtsp {
    private var serviceStarted = false
    private var streamAllowed= false
    private var ip: Int= 0

    private lateinit var settings: Settings

    private var rtspServerCamera1: RtspServerCamera1?= null
    private var wifimon: WifiMonitor?= null

    private val TAG = javaClass.simpleName

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegraund()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        wifimon?.disable()
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(ev: BaseEvent){
        if(ev.type== EventEnum.STREAM){
            streamAllowed= ev.bundle.getBoolean("stream")
            Log.i(TAG, "stream = $streamAllowed")
            if(streamAllowed) {
                if (rtspServerCamera1 == null)
                    startStream()
            }
            else
                stopStream()
        }
        if(ev.type== EventEnum.SETTINGS){
            Utils.sendConnString(ip)
            stopStream()
            settings= ev.bundle.getParcelable("settings")!!
            settings.resolutions= getResolutions()
            Log.i(TAG, "settings received, restart stream")
            startStream()
        }
    }

    private val wifiListener= object: WifiMonitor.StateChanged{
        override fun onChanged(available: Boolean, ip: Int) {
            Log.i(TAG, "wifi enabled = $available, ip= $ip")
            this@CamService.ip = ip
            Utils.storeInt(this@CamService, "ip", ip)
            Utils.sendConnString(this@CamService.ip)
            if(ip== 0)
                stopStream()
            else
                startStream()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        settings= Utils.loadSettings(this)
        Log.i(TAG, "settings loaded at start service")
        if (!serviceStarted) {
            settings.resolutions= getResolutions()
            serviceStarted = true
            streamAllowed= true
            startForegraund()
            wifimon= WifiMonitor(wifiListener)
            wifimon?.enable(this.applicationContext)
            val workReq= PeriodicWorkRequest.Builder(MyWorker::class.java, 60, TimeUnit.MINUTES,
                59, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueue(workReq)
            Log.i(TAG, "service started")
        }

        Utils.sendConnString(ip)
        return START_STICKY
    }

    private fun startForegraund() {
        Log.i(TAG, "---------start foreground")
        val channelId: String = getString(R.string.app_name)
        val mNotificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mNotificationManager.createNotificationChannel(mChannel)
        }
        val notificationIntent =
            Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val mBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WebCamCloud")
            .setContentText("")
        mBuilder.setContentIntent(contentIntent)
        startForeground(111, mBuilder.build())
    }

    private fun getResolutions(): List<Camera.Size>{
        val cam= RtspCamera1(this, null)
        val resolutions= if(settings.camera== 0) cam.resolutionsBack else cam.resolutionsFront
        val strResolutions= resolutions.map {
            "${it.width}x${it.height}"
        }
        val bundle= Bundle()
        bundle.putStringArray("resolutions", strResolutions.toTypedArray())
        EventBus.getDefault().postSticky(BaseEvent(EventEnum.RESOLUTION, bundle))
        return resolutions
    }
    private fun stopStream(){
        rtspServerCamera1?.let{
            it.stopStream()
            Log.i(TAG, "camera and stream stopped")
            Utils.storeBoolean(this, "streamStarted", false)
            rtspServerCamera1= null
        }
    }

    private fun startStream() {
        if(ip!= 0 && streamAllowed){
            startCamera()
            Utils.storeBoolean(this, "streamStarted", true)
        }
    }

    private fun startCamera() {

        rtspServerCamera1 = RtspServerCamera1(this, this, settings.port)
        Log.i(TAG, "camera created")

        rtspServerCamera1?.let{

            Log.i(TAG, "restart camera and stream with current settings")
            it.setAuthorization(settings.login, settings.password)
            val isFacingBack= settings.camera== 0
            val rotation = 180
            var w = 640
            var h = 480
            settings.resolutions?.let{
                if(settings.resolution>= 0) {
                    w = it[settings.resolution].width
                    h = it[settings.resolution].height
                }
            }
            if (it.isRecording || it.prepareAudio()
                && it.prepareVideo(w, h, settings.rate, 1024 * 1024, false, rotation)) {
                it.startStream()
                if((it.isFrontCamera && isFacingBack) || (!it.isFrontCamera && !isFacingBack))
                    it.switchCamera()
            }
            else {
                Toast.makeText(
                    this,
                    "Error preparing stream, This device cant do it",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onConnectionSuccessRtsp() {
        Log.i(TAG, "onConnectionSuccessRtsp")
    }

    override fun onConnectionFailedRtsp(reason: String?) {
        Log.i(TAG, "onConnectionFailedRtsp")
        rtspServerCamera1?.stopStream()
    }

    override fun onNewBitrateRtsp(bitrate: Long) {
        Log.i(TAG, "onNewBitrateRtsp: $bitrate")
    }

    override fun onDisconnectRtsp() {
        Log.i(TAG, "onDisconnectRtsp")
    }

    override fun onAuthErrorRtsp() {
        Log.i(TAG, "onAuthErrorRtsp")
        rtspServerCamera1?.stopStream()
    }

    override fun onAuthSuccessRtsp() {
        Log.i(TAG, "onAuthSuccessRtsp")
    }

    inner class MyWorker(val ctx: Context, val params: WorkerParameters): Worker(ctx, params) {

        override fun doWork(): Result {
            stopStream()
            startStream()
            return Result.success()
        }
    }
}