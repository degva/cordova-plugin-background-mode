/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package de.appplant.cordova.plugin.background;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
//import android.os.Build;
import android.os.Handler;
import android.os.IBinder;


import android.util.Log;
/*
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
*/

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
public class ForegroundService extends Service {

    // Fixed ID for the 'foreground' notification
    private static final int NOTIFICATION_ID = -574543954;

    // Scheduler to exec periodic tasks
    final Timer scheduler = new Timer();

    // Used to keep the app alive
    TimerTask keepAliveTask;
    NotificationManager nNM;
    Notification.Builder notification;

    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate () {
        super.onCreate();
        nNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        keepAwake();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sleepWell();
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    public void keepAwake() {
    	final Handler handler = new Handler();
        final JSONObject settings = BackgroundMode.settings;
        String title = settings.optString("title", "");
        // String text = settings.optString("text", "");
        String ticker = settings.optString("ticker", "");
        Boolean resume = settings.optBoolean("resume");
        
        final int secs = Integer.parseInt(settings.optString("seconds", "217"));
        
        Log.i("keepAwake", "this is the min: " + secs);

        // startForeground(NOTIFICATION_ID, makeNotification());
        final int min = secs / 60;
        makeNotification(title, min, ticker, resume);
        
        keepAliveTask = new TimerTask() {
        	// int minute = settings.optInt("minutes");
        	int minute = min;
        	int i = secs;
        	            
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Nothing to do here
                        Log.i("BackgroundMode", "Second=" + i);
                    	--i;
                    	if (i % 60 == 0) {
                    		--minute;
                    		updateNotification(minute);
                    	}
                    }
                });
            }
        };

        scheduler.schedule(keepAliveTask, 0, 1000);
    }

    /**
     * Stop background mode.
     */
    private void sleepWell() {
        // stopForeground(true);
        keepAliveTask.cancel();
        Log.i("sleepWell", "Destroying the Notification");
        nNM.cancel(NOTIFICATION_ID);
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @return
     *      A local ongoing notification which pending intent is bound to the
     *      main activity.
     */
    @SuppressLint("NewApi")
    // @SuppressWarnings("deprecation")
    // private Notification makeNotification() {
    private void makeNotification(String title, int min, String ticker, Boolean resume) {
        // JSONObject settings = BackgroundMode.settings;
        Context context     = getApplicationContext();
        String pkgName      = context.getPackageName();
        Intent intent       = context.getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        notification = new Notification.Builder(context)
            .setContentTitle(title)
            .setContentText("There's " + min + " min left")
            .setTicker(ticker)
            .setOngoing(true)
            .setSmallIcon(getIconResId());

        if (intent != null && resume) {

            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            notification.setContentIntent(contentIntent);
        }
        /*
        if (Build.VERSION.SDK_INT < 16) {
            // Build notification for HoneyComb to ICS
            return notification.getNotification();
        } else {
            // Notification for Jellybean and above
            return notification.build();
        }
        */
        Log.i("makeNotification", "Doing the Notification! :D");
        nNM.notify(NOTIFICATION_ID, notification.build());
    }
    
    public void updateNotification(int i) {
    	notification.setContentText("There's " + i + " Minutes Left");
    	Log.i("updateNotification", "Changed the text to: " + i);
    	nNM.notify(NOTIFICATION_ID, notification.build());
    	
    }
    
    /**
     * Retrieves the resource ID of the app icon.
     *
     * @return
     *      The resource ID of the app icon
     */
    private int getIconResId () {
        Context context = getApplicationContext();
        Resources res   = context.getResources();
        String pkgName  = context.getPackageName();

        int resId;
        resId = res.getIdentifier("icon", "drawable", pkgName);

        return resId;
    }
}
