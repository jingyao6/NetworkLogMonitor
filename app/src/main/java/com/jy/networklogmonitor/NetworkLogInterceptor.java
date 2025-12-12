package com.jy.networklogmonitor;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class NetworkLogInterceptor implements Interceptor {
    private static final CopyOnWriteArrayList<NetworkLog> logList = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<LogChangeListener> listeners = new CopyOnWriteArrayList<>();

    // 日志变化监听器接口
    public interface LogChangeListener {
        void onLogChanged();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.currentTimeMillis();

        // 创建日志对象
        NetworkLog log = new NetworkLog(request.url().toString(), request.method());

        // 记录请求体
        RequestBody requestBody = request.body();
        if (requestBody != null) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            log.setRequestBody(buffer.readUtf8());
        }

        // 记录请求头
        log.setHeaders(request.headers().toString());

        // 执行请求
        Response response = chain.proceed(request);
        long endTime = System.currentTimeMillis();

        // 记录响应信息
        log.setResponseTime(endTime - startTime);
        log.setStatusCode(response.code());

        // 记录响应体
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            MediaType mediaType = responseBody.contentType();
            String bodyString = responseBody.string();
            log.setResponseBody(bodyString);
            // 重新构建响应体，因为它只能被读取一次
            response = response.newBuilder()
                    .body(ResponseBody.create(mediaType, bodyString))
                    .build();
        }

        // 添加到日志列表
        logList.add(log);
        
        // 通知所有监听器日志变化
        notifyLogChanged();

        return response;
    }

    // 获取日志列表
    public static CopyOnWriteArrayList<NetworkLog> getLogList() {
        return logList;
    }

    // 清除日志列表
    public static void clearLogs() {
        logList.clear();
        // 通知所有监听器日志变化
        notifyLogChanged();
    }
    
    // 注册日志变化监听器
    public static void registerLogChangeListener(LogChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    // 取消注册日志变化监听器
    public static void unregisterLogChangeListener(LogChangeListener listener) {
        listeners.remove(listener);
    }
    
    // 通知所有监听器日志变化
    private static void notifyLogChanged() {
        for (LogChangeListener listener : listeners) {
            listener.onLogChanged();
        }
    }
}
