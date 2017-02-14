package com.silverlit.onenetedp.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;
//import com.chinamobile.iot.onenet.edp.toolbox.EdpService;
import com.silverlit.onenetedp.model.OneNetEDPClient;

public class ServiceOneNET extends Service {
    public ServiceOneNET() {
    }

    public static final String TAG = "EdpService";

    private ServiceOneNET.HeartbeatReceiver mHeartbeatReceiver = new ServiceOneNET.HeartbeatReceiver();
    private OneNetEDPClient mEdpClient;

    public static void start(Context context) {
        Intent intent = new Intent(context, ServiceOneNET.class);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ServiceOneNET.class);
        context.stopService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEdpClient = OneNetEDPClient.getInstance();
        mHeartbeatReceiver.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent broadcastIntent = new Intent(EdpClient.ACTION_HEARTBEAT);
        sendBroadcast(broadcastIntent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mHeartbeatReceiver.unregister(this);
        super.onDestroy();
    }

    private class HeartbeatReceiver extends BroadcastReceiver {

        public void register(Context context) {
            IntentFilter filter = new IntentFilter(EdpClient.ACTION_HEARTBEAT);
            context.registerReceiver(this, filter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mEdpClient.sendHeartbeat();
            mEdpClient.setupAlarm(context);
            Log.d(TAG,"heartbeat");
        }
    }
}
