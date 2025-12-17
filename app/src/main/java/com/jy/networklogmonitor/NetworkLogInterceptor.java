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
    
    // 加密处理接口
    public interface EncryptionHandler {
        // 加密请求体
        String encryptRequestBody(String url,RequestBody originalBody);
        
        // 解密响应体
        String decryptResponseBody(String url,ResponseBody responseBody);
    }
    
    // 加密处理器实例
    private static EncryptionHandler encryptionHandler;
    
    // 设置加密处理器
    public static void setEncryptionHandler(EncryptionHandler handler) {
        encryptionHandler = handler;
    }
    
    // 获取加密处理器
    public static EncryptionHandler getEncryptionHandler() {
        return encryptionHandler;
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
            // 使用加密处理器加密请求体（如果存在）
            if (encryptionHandler != null) {
                try {
                    requestBodyString = encryptionHandler.encryptRequestBody(request.url().toString(), requestBody);
                } catch (Exception e) {
                    // 如果加密失败，使用原始请求体
                    e.printStackTrace();
                }
            }
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
                // 使用加密处理器解密响应体（如果存在）
                if (encryptionHandler != null) {
                    try {
                        // 重新构建一个 ResponseBody 用于解密，因为原 ResponseBody 已经被读取
                        ResponseBody decryptBody = ResponseBody.create(mediaType, bodyString);
                        bodyString = encryptionHandler.decryptResponseBody(request.url().toString(), decryptBody);
                    } catch (Exception e) {
                        // 如果解密失败，使用原始响应体
                        e.printStackTrace();
                    }
                }
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
