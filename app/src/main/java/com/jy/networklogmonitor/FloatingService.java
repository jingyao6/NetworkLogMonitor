package com.jy.networklogmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.util.List;

public class FloatingService extends Service implements NetworkLogInterceptor.LogChangeListener {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private boolean isViewAdded = false;
    private Handler handler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        // 创建通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "network_log_monitor",
                    "Network Log Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // 创建前台通知
        Notification notification = new NotificationCompat.Builder(this, "network_log_monitor")
                .setContentTitle("Network Log Monitor")
                .setContentText("Running in background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        // 启动前台服务
        startForeground(1, notification);

        // 初始化悬浮窗
        initializeFloatingView();
        
        // 注册日志变化监听器
        NetworkLogInterceptor.registerLogChangeListener(this);
        
        // 启动应用前后台状态检查
        startAppStatusCheck();
    }

    private void initializeFloatingView() {
        // 获取WindowManager服务
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 加载悬浮窗布局
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_widget, null);

        // 设置悬浮窗参数
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // 设置悬浮窗初始位置
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        // 设置悬浮窗点击事件
        floatingView.findViewById(R.id.floating_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击悬浮窗，打开日志列表
                Intent intent = new Intent(FloatingService.this, LogListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        // 设置悬浮窗拖拽事件
        floatingView.findViewById(R.id.floating_button).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return false;
                    default:
                        return false;
                }
            }
        });

        // 更新日志数量显示
        updateLogCount();
        
        // 检查应用是否在前台，决定是否显示悬浮窗
        checkAndUpdateFloatingViewVisibility();
    }

    private void updateLogCount() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (floatingView != null) {
                    TextView logCountText = floatingView.findViewById(R.id.log_count);
                    int count = NetworkLogInterceptor.getLogList().size();
                    logCountText.setText(String.valueOf(count));
                }
            }
        });
    }
    
    private void startAppStatusCheck() {
        // 定时检查应用是否在前台
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndUpdateFloatingViewVisibility();
                // 每1秒检查一次
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    private void checkAndUpdateFloatingViewVisibility() {
        if (isAppInForeground()) {
            if (!isViewAdded) {
                // 应用在前台，显示悬浮窗
                windowManager.addView(floatingView, params);
                isViewAdded = true;
            }
        } else {
            if (isViewAdded) {
                // 应用在后台，隐藏悬浮窗
                windowManager.removeView(floatingView);
                isViewAdded = false;
            }
        }
    }
    
    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        
        // 获取当前应用包名
        String packageName = getPackageName();
        
        // 检查当前应用是否在前台运行
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 取消注册日志变化监听器
        NetworkLogInterceptor.unregisterLogChangeListener(this);
        // 移除悬浮窗
        if (isViewAdded && floatingView != null) {
            windowManager.removeView(floatingView);
            isViewAdded = false;
        }
        // 移除所有handler任务
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
    
    @Override
    public void onLogChanged() {
        // 日志变化时更新计数显示
        updateLogCount();
    }
}
