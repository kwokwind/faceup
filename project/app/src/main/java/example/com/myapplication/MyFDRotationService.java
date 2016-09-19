/*
 * Copyright (C) 2012 Motorola Mobility Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package example.com.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.Binder;
import android.os.IBinder;

public class MyFDRotationService extends Service {
    public static final String TAG = "MyFDRotationService";

    private static boolean mIsDetecting = false;
    private MyFDRotationDisplay mMyFDRotationDisplay = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            String action = intent.getAction();
            if (action == null || action.length() == 0)
                return;

            Log.d(TAG, "BroadcastReceiver action="+ action);

            // PowerManager pm = (PowerManager)
            // context.getSystemService(Context.POWER_SERVICE);
            // boolean screen = pm.isScreenOn();

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "display is on, start face detection!");
                if (mMyFDRotationDisplay != null && !mIsDetecting) {
                    mMyFDRotationDisplay.startDetection();
                    Log.d(TAG, "face detection is started!");
                    mIsDetecting = true;
                }
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "display is off, stop face detection!");
                if (mMyFDRotationDisplay != null && mIsDetecting) {
                    mMyFDRotationDisplay.stopDetection();
                    Log.d(TAG, "face detection is stopped!");
                    mIsDetecting = false;
                }
            }
        }
    };

    private void eventRegister() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, intentFilter);
    }

    private void eventUnregister() {
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        if (mMyFDRotationDisplay == null) {
            mMyFDRotationDisplay = new MyFDRotationDisplay(this);
        }

        if (!mIsDetecting) {
            mMyFDRotationDisplay.startDetection();
            mIsDetecting = true;
        }

        eventRegister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        eventUnregister();

        if (mMyFDRotationDisplay != null && mIsDetecting) {
            mMyFDRotationDisplay.stopDetection();
            mIsDetecting = false;
        }

        if (mMyFDRotationDisplay != null) {
            mMyFDRotationDisplay.destroy();
            mMyFDRotationDisplay = null;
        }
    }

    public class MyFDRotationBinder extends Binder {
        /*
        MyFDRotationService getService() {
            return MyFDRotationService.this;
        }
        */
    }
    private final IBinder mBinder = new MyFDRotationBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent arg) {
        return true;
    }
}