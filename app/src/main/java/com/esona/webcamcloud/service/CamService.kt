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
import com.esona.webcamcloud.ui.MainActivity
import com.esona.webcamcloud.util.BaseEventJ
import com.esona.webcamcloud.util.WifiMonitor
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerCamera1
import org.greenrobot.eventbus.EventBus

class CamService : Service(), ConnectCheckerRtsp {
    private var started = false

    private lateinit var rtspServerCamera1: RtspServerCamera1
    private var wifimon: WifiMonitor?= null

    private val TAG = javaClass.simpleName

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        wifimon?.disable()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            startFore()
            wifimon= WifiMonitor()
            wifimon?.enable(this.applicationContext)
//            init();
        } else {
            EventBus.getDefault().post(BaseEventJ(rtspServerCamera1.getEndPointConnection()))
        }
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

    private fun init() {
        val disp =
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val rect = Rect()
        disp.getRectSize(rect)
        startCamera(rect.width(), rect.height())
    }

    private fun startCamera(width: Int, height: Int) {
/*
        if (rtspServerCamera1 != null) {
            rtspServerCamera1.stopPreview()
            rtspServerCamera1.stopStream()
        }
*/
        rtspServerCamera1 = RtspServerCamera1(this, this, 1935)
        rtspServerCamera1.setAuthorization("test", "test")
        rtspServerCamera1.startPreview(width, height)
        val rotation = 180
        if (rtspServerCamera1.isRecording || rtspServerCamera1.prepareAudio()
            && rtspServerCamera1.prepareVideo(640, 480, 30, 1024 * 1024, false, rotation)
        ) {
            rtspServerCamera1.startStream()
            EventBus.getDefault().post(BaseEventJ(rtspServerCamera1.getEndPointConnection()))
        } else {
            Toast.makeText(
                this,
                "Error preparing stream, This device cant do it",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onConnectionSuccessRtsp() {
        Log.i(TAG, "onConnectionSuccessRtsp")
    }

    override fun onConnectionFailedRtsp(reason: String?) {
        Log.i(TAG, "onConnectionFailedRtsp")
        rtspServerCamera1.stopStream()
    }

    override fun onNewBitrateRtsp(bitrate: Long) {
        Log.i(TAG, "onNewBitrateRtsp: $bitrate")
    }

    override fun onDisconnectRtsp() {
        Log.i(TAG, "onDisconnectRtsp")
    }

    override fun onAuthErrorRtsp() {
        Log.i(TAG, "onAuthErrorRtsp")
        rtspServerCamera1.stopStream()
    }

    override fun onAuthSuccessRtsp() {
        Log.i(TAG, "onAuthSuccessRtsp")
    }

}