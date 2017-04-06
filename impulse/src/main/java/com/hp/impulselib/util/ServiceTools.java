package com.hp.impulselib.util;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.List;

public class ServiceTools {
    private static String LOG_TAG = ServiceTools.class.getName();

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        if (context == null || serviceClass == null) return false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            Log.d(LOG_TAG, String.format("Service: %s", runningServiceInfo.service.getClassName()));
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())) {
                return true;
            }
        }
        return false;
    }
}
