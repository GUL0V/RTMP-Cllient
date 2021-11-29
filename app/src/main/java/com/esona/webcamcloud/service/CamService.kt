package com.esona.webcamcloud.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.Camera
import android.os.*
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.esona.webcamcloud.ConnectCheckerRtp
import com.esona.webcamcloud.R
import com.esona.webcamcloud.data.BaseEvent
import com.esona.webcamcloud.data.EventEnum
import com.esona.webcamcloud.data.Settings
import com.esona.webcamcloud.ui.MainActivity
import com.esona.webcamcloud.util.Utils
import com.esona.webcamcloud.util.WifiMonitor
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtplibrary.base.Camera2Base
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.rtsp.RtspCamera1
import com.pedro.rtplibrary.view.OpenGlView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit




class CamService : Service() {
    private var serviceStarted = false
    private var streamAllowed= false
    private var ip: Int= 0
    private var handler: Handler? = null
    private var runnable: Runnable? = null

    private lateinit var settings: Settings

    private var wifimon: WifiMonitor?= null

    private val TAG = javaClass.simpleName
    companion object {
        const val TAG = "RtpService"
        private const val channelId = "rtpStreamChannel"
        private const val notifyId = 123456
        private var notificationManager: NotificationManager? = null
        var camera2Base: Camera2Base? = null
        private var openGlView: OpenGlView? = null
        private var contextApp: Context? = null

        fun setView(openGlView: OpenGlView) {
            this.openGlView = openGlView
            camera2Base?.replaceView(openGlView)
        }

        fun setView(context: Context) {
            contextApp = context
            this.openGlView = null
            camera2Base?.replaceView(context)
        }

        fun startPreview() {
            camera2Base?.startPreview()
        }

        fun init(context: Context) {
            contextApp = context
            if (camera2Base == null) camera2Base = RtmpCamera2(context, true, connectCheckerRtp)
        }

        fun stopStream() {
            if (camera2Base != null) {
                if (camera2Base!!.isStreaming) {
                    camera2Base!!.stopStream()
                        Log.i(TAG, "camera and stream stopped")
                    contextApp?.let { Utils.storeBoolean(it, "streamStarted", false) }
                    camera2Base = null;
                }
                }
            }


        fun stopPreview() {
            if (camera2Base != null) {
                if (camera2Base!!.isOnPreview) camera2Base!!.stopPreview()
            }
        }
        private fun startStreamRtp(endpoint: String) {
            if (!camera2Base!!.isStreaming) {
                if (prepareEncoders()) {
                    camera2Base!!.startStream(endpoint)
                }
            } else {
                // showNotification("You are already streaming :(")
            }
        }
        fun getState():Boolean{
            return camera2Base!!.isStreaming
        }

        private fun prepareEncoders(): Boolean {
            val resolution: Size = camera2Base!!.getResolutionsBack().get(0)
            val width = resolution.width
            val height = resolution.height
            return camera2Base?.prepareVideo(
                1280, 720, 30, 2500 * 1024, CameraHelper.getCameraOrientation(contextApp))== true
                    &&
                    camera2Base!!.prepareAudio(64 * 1024, 32000, true, false, false)
        }

        private val connectCheckerRtp = object : ConnectCheckerRtp {
            override fun onConnectionStartedRtp(rtpUrl: String) {
                //   showNotification("Stream connection started")
            }

            override fun onConnectionSuccessRtp() {
                // showNotification("Stream started")
                Log.e(TAG, "RTP service destroy")
            }

            override fun onNewBitrateRtp(bitrate: Long) {

            }

            override fun onConnectionFailedRtp(reason: String) {
                //  showNotification("Stream connection failed")
                Log.e(TAG, "RTP service destroy")
            }

            override fun onDisconnectRtp() {
                //  showNotification("Stream stopped")
            }

            override fun onAuthErrorRtp() {
                // showNotification("Stream auth error")
            }

            override fun onAuthSuccessRtp() {
                // showNotification("Stream auth success")
            }
        }


    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegraund()
        EventBus.getDefault().register(this)
    }


    override fun onDestroy() {
        Utils.storeBoolean(this, "streamStarted", false)
        Log.d("TAGcheckstart", "zhies: ")
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
                if (camera2Base == null) {
                    startStream()
                    Log.d("TAGcheckstart", "firste: ")
                }
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
            if (ip == 0)
                stopStream()
            else {
                startStream()
                Log.d("TAGcheckstart", "wifi: ")
            }
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
            Log.i(TAG, "service started")
        }

        Utils.sendConnString(ip)
        return START_STICKY
    }
    fun autoRestartStream(){
        if(Utils.loadBoolean(applicationContext, "streamStarted")){
            Handler(Looper.getMainLooper()).postDelayed({
                stopStream()
                Log.i(TAG, "qwertyuioasdgklcvbnm"+settings.auto)
                startStream()
            }, (settings.auto*60*1000).toLong())
        }
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
        val cam= RtmpCamera1(this, connectCheckerRtp)
        val resolutions= if(settings.camera== 0) cam.resolutionsBack else cam.resolutionsFront
        val strResolutions= resolutions.filter { it.width<=1280 && it.height<=720 }.map {
             "${it.width} x ${it.height}"
        }
        val bundle= Bundle()
        bundle.putStringArray("resolutions", strResolutions.toTypedArray())
        EventBus.getDefault().postSticky(BaseEvent(EventEnum.RESOLUTION, bundle))
        return resolutions
    }



    private fun startStream() {
        if(ip!= 0 && streamAllowed){
            startCamera()
            Utils.storeBoolean(this, "streamStarted", true)
            autoRestartStream();
        }
    }

    private fun startCamera() {

        Log.i(TAG, "camera created")

        camera2Base?.let{ it ->

            Log.i(TAG, "restart camera and stream with current settings")
            //it.setAuthorization(settings.login, settings.password)
            val isFacingBack= settings.camera== 0
            val rotation = 180
            var w = 640
            var h = 480
            settings.resolutions?.let{
                if(settings.resolutionW>= 0) {
                    if(settings.resolutionW==1280){
                        w = 640
                        h = 480
                    }
                    else if(settings.resolutionH==1280){
                        w = 480
                        h = 640
                    }
                    else{
                        w = settings.resolutionW
                        h = settings.resolutionH
                    }
                    Log.d(TAG, "startCamera: $w $h $isFacingBack")
                }
            }
            if (it.isRecording || it.prepareAudio()
                && it.prepareVideo(w, h, settings.rate, 1024 * 1024,  rotation)) {
                it.startStream("rtmp://media.videosurveillance.cloud:1935/live/10_d67d8ab4f4c10bf22aa353e27879133c?psk=secretPassword")
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






}