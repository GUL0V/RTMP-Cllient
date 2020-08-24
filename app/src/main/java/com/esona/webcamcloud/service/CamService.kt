package com.esona.webcamcloud.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.ui.MainActivity
import com.esona.webcamcloud.util.Utils
import com.esona.webcamcloud.util.WifiMonitor
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        wifimon?.disable()
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(ev: BaseEvent){
        if(ev.type== EventEnum.STREAM){
            val stream= ev.bundle.getBoolean("stream")
            Log.i(TAG, "stream = $stream")
            if(stream)
                startStream()
            else
                stopStream()
        }
        if(ev.type== EventEnum.SETTINGS){
            settings= ev.bundle.getParcelable("settings")!!
            Log.i(TAG, "settings received, restart stream")
            stopStream()
            startStream()
        }
    }

    private val wifiListener= object: WifiMonitor.StateChanged{
        override fun onChanged(available: Boolean, ip: Int) {
            Log.i(TAG, "wifi enabled = $available, ip= $ip")
            this@CamService.ip = ip
            Utils.sendConnString(this@CamService.ip)
            if(ip== 0)
                stopStream()
            else
                startStream()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let{
            settings= it.getParcelableExtra("settings")!!
            Log.i(TAG, "settings received at start service")
        }
        if (!serviceStarted) {
            serviceStarted = true
            startFore()
            wifimon= WifiMonitor(wifiListener)
            wifimon?.enable(this.applicationContext)
            Log.i(TAG, "service started")
        }

        Utils.sendConnString(ip)
        return START_STICKY
    }

    private fun startFore() {
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
        startForeground(0, mBuilder.build())
    }

    private fun stopStream(){
        rtspServerCamera1?.let{
            it.stopPreview()
            it.stopStream()
            Log.i(TAG, "camera and stream stopped")
        }
    }

    private fun startStream() {
        if(ip> 0 && streamAllowed){
            val disp =
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val rect = Rect()
            disp.getRectSize(rect)
            startCamera(rect.width(), rect.height())
        }
    }

    private fun startCamera(width: Int, height: Int) {
        if(rtspServerCamera1== null) {
            rtspServerCamera1 = RtspServerCamera1(this, this, settings.port)
            Log.i(TAG, "camera created")
        }
        rtspServerCamera1?.let{

            Log.i(TAG, "restart camera and stream with current settings")
            it.setAuthorization(settings.login, settings.password)

            val facing= if(settings.camera== 0) CameraHelper.Facing.BACK else CameraHelper.Facing.FRONT
            it.startPreview(facing, width, height)

            val rotation = 180
            if (it.isRecording || it.prepareAudio()
                && it.prepareVideo(640, 480, settings.rate, 1024 * 1024, false, rotation)) {
                it.startStream()
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

}