package com.jy.networklogmonitor;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.HttpUrl;
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
    

    // 加密处理器实例
    private static Interceptor encryptionInterceptor;

    public static Interceptor getEncryptionInterceptor() {
        return encryptionInterceptor;
    }

    public static void setEncryptionInterceptor(Interceptor encryptionInterceptor) {
        NetworkLogInterceptor.encryptionInterceptor = encryptionInterceptor;
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
        String requestBodyString = null;
        if (requestBody != null) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            requestBodyString = buffer.readUtf8();
            // 直接记录原始请求体，不进行加密处理
            log.setRequestBody(requestBodyString);
        }

        // 记录请求头
        log.setHeaders(request.headers().toString());

        // 执行请求并处理可能的错误
        Response response = null;
        try {
            // 执行请求
            response = chain.proceed(request);
            long endTime = System.currentTimeMillis();

            // 记录响应信息
            log.setResponseTime(endTime - startTime);
            log.setStatusCode(response.code());

            // 记录响应体
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                MediaType mediaType = responseBody.contentType();
                String bodyString = responseBody.string();
                // 直接记录原始响应体，不进行解密处理
                log.setResponseBody(bodyString);
                // 重新构建响应体，因为它只能被读取一次
                response = response.newBuilder()
                        .body(ResponseBody.create(mediaType, bodyString))
                        .build();
            }
        } catch (IOException e) {
            // 处理请求失败情况
            long endTime = System.currentTimeMillis();
            log.setResponseTime(endTime - startTime);
            log.setStatusCode(0); // 使用0表示请求失败
            log.setErrorMessage(e.getMessage());
            log.setResponseBody("Request failed: " + e.getMessage());
            
            // 重新抛出异常，确保调用者能感知到错误
            throw e;
        } finally {
            // 添加到日志列表（无论成功或失败）
            logList.add(log);
            
            // 通知所有监听器日志变化
            notifyLogChanged();
        }

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
