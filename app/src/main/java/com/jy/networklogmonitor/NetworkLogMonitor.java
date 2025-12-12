package com.jy.networklogmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class NetworkLogMonitor {
    private static boolean isInitialized = false;

    public static void initialize(Context context) {
        if (isInitialized) {
            return;
        }

        // 检查并请求悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, 1001);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }

        // 启动悬浮窗服务
        Intent serviceIntent = new Intent(context, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        isInitialized = true;
    }

    public static NetworkLogInterceptor getInterceptor() {
        return new NetworkLogInterceptor();
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(context, FloatingService.class);
        context.stopService(serviceIntent);
        isInitialized = false;
    }
}
