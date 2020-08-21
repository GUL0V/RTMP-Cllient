package com.esona.webcamcloud.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.esona.webcamcloud.R;
import com.esona.webcamcloud.ui.MainActivity;
import com.esona.webcamcloud.util.BaseEventJ;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import com.pedro.rtspserver.RtspServerCamera1;

import org.greenrobot.eventbus.EventBus;

public class CamService extends Service implements ConnectCheckerRtsp {
    private RtspServerCamera1 rtspServerCamera1 = null;
    private boolean started;
    private Rect rect;

    private String TAG= getClass().getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!started){
            started= true;
            startFore();
            init();
        }
        else{
            EventBus.getDefault().post(new BaseEventJ(rtspServerCamera1.getEndPointConnection()));
        }
        return START_STICKY;
    }

    private void startFore(){
        String channelId = getString(R.string.app_name);
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("test")
                .setContentText("test");

        mBuilder.setContentIntent(contentIntent);

        startForeground(0, mBuilder.build());
    }

    private void init(){
        Display disp= ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        rect= new Rect();
        disp.getRectSize(rect);

        startCamera(rect.width(), rect.height());

    }

    private void startCamera(int width, int height){
        if(rtspServerCamera1!= null){
            rtspServerCamera1.stopPreview();
            rtspServerCamera1.stopStream();
        }
        rtspServerCamera1 = new RtspServerCamera1(this, this, 1935);
        rtspServerCamera1.setAuthorization("test", "test");
        rtspServerCamera1.startPreview(width, height);
        int rotation = 180;
        if (rtspServerCamera1.isRecording() || rtspServerCamera1.prepareAudio()
                && rtspServerCamera1.prepareVideo(640, 480, 30, 1024*1024, false, rotation)) {
            rtspServerCamera1.startStream();
            EventBus.getDefault().post(new BaseEventJ(rtspServerCamera1.getEndPointConnection()));
        } else {
            Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
                    .show();
        }

    }

    @Override
    public void onConnectionSuccessRtsp() {
        Log.i(TAG, "onConnectionSuccessRtsp");
    }

    @Override
    public void onConnectionFailedRtsp(String reason) {
        Log.i(TAG, "onConnectionFailedRtsp");
        rtspServerCamera1.stopStream();
    }

    @Override
    public void onNewBitrateRtsp(long bitrate) {
        Log.i(TAG, "onNewBitrateRtsp: "+ bitrate);
    }

    @Override
    public void onDisconnectRtsp() {
        Log.i(TAG, "onDisconnectRtsp");
    }

    @Override
    public void onAuthErrorRtsp() {
        Log.i(TAG, "onAuthErrorRtsp");
        rtspServerCamera1.stopStream();
    }

    @Override
    public void onAuthSuccessRtsp() {
        Log.i(TAG, "onAuthSuccessRtsp");
    }


}
