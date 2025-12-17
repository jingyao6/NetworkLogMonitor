package com.jy.networklogmonitor;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LogDetailActivity extends AppCompatActivity {
    private TextView methodText;
    private TextView statusCodeText;
    private TextView responseTimeText;
    private EditText urlText;
    private TextView timestampText;
    private EditText headersText;
    private EditText requestBodyText;
    private TextView responseBodyText;
    private TextView errorMessageText;
    private LinearLayout errorContainer;
    private Button editButton;
    private Button resendButton;
    private boolean isEditing = false;

    private NetworkLog currentLog;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_detail);

        // 初始化视图
        methodText = findViewById(R.id.method_text);
        statusCodeText = findViewById(R.id.status_code_text);
        responseTimeText = findViewById(R.id.response_time_text);
        urlText = findViewById(R.id.url_text);
        timestampText = findViewById(R.id.timestamp_text);
        headersText = findViewById(R.id.headers_text);
        requestBodyText = findViewById(R.id.request_body_text);
        responseBodyText = findViewById(R.id.response_body_text);
        errorMessageText = findViewById(R.id.error_message_text);
        errorContainer = findViewById(R.id.error_container);
        editButton = findViewById(R.id.edit_button);
        resendButton = findViewById(R.id.resend_button);

        // 获取日志位置
        int logPosition = getIntent().getIntExtra("log_position", -1);
        if (logPosition == -1) {
            finish();
            return;
        }

        // 获取日志列表并显示详情
        CopyOnWriteArrayList<NetworkLog> logList = NetworkLogInterceptor.getLogList();
        if (logPosition < logList.size()) {
            currentLog = logList.get(logPosition);
            displayLogDetails(currentLog);
        }

        // 设置编辑按钮点击事件
        editButton.setOnClickListener(v -> toggleEditMode());

        // 设置重新发送按钮点击事件
        resendButton.setOnClickListener(v -> resendRequest());


    }

    private void displayLogDetails(NetworkLog log) {
        // 设置基本信息
        methodText.setText(log.getMethod());
        statusCodeText.setText(String.valueOf(log.getStatusCode()));
        responseTimeText.setText(log.getResponseTime() + "ms");
        urlText.setText(log.getUrl());
        timestampText.setText(dateFormat.format(log.getTimestamp()));

        // 设置请求头
        headersText.setText(log.getHeaders() != null ? log.getHeaders() : "No headers");

        // 设置请求体
        requestBodyText.setText(log.getRequestBody() != null ? log.getRequestBody() : "No request body");

        // 设置响应体
        responseBodyText.setText(log.getResponseBody() != null ? log.getResponseBody() : "No response body");

        // 设置错误信息
        if (log.getErrorMessage() != null) {
            errorMessageText.setText(log.getErrorMessage());
            errorContainer.setVisibility(View.VISIBLE);
        } else {
            errorContainer.setVisibility(View.GONE);
        }

        // 根据状态码设置不同颜色
        if (log.getStatusCode() >= 200 && log.getStatusCode() < 300) {
            statusCodeText.setBackgroundResource(R.drawable.status_success_background);
        } else if (log.getStatusCode() >= 300 && log.getStatusCode() < 400) {
            statusCodeText.setBackgroundResource(R.drawable.status_redirect_background);
        } else if (log.getStatusCode() >= 400 && log.getStatusCode() < 500) {
            statusCodeText.setBackgroundResource(R.drawable.status_client_error_background);
        } else if (log.getStatusCode() == 0) {
            // 请求失败
            statusCodeText.setBackgroundResource(R.drawable.status_server_error_background);
            statusCodeText.setText("ERROR");
        } else {
            statusCodeText.setBackgroundResource(R.drawable.status_server_error_background);
        }

        // 根据请求方法设置不同颜色
        switch (log.getMethod()) {
            case "GET":
                methodText.setBackgroundResource(R.drawable.method_get_background);
                break;
            case "POST":
                methodText.setBackgroundResource(R.drawable.method_post_background);
                break;
            case "PUT":
                methodText.setBackgroundResource(R.drawable.method_put_background);
                break;
            case "DELETE":
                methodText.setBackgroundResource(R.drawable.method_delete_background);
                break;
            default:
                methodText.setBackgroundResource(R.drawable.method_background);
                break;
        }
    }

    private void toggleEditMode() {
        isEditing = !isEditing;
        
        if (isEditing) {
            // 进入编辑模式
            urlText.setFocusable(true);
            urlText.setFocusableInTouchMode(true);
            urlText.setClickable(true);
            
            headersText.setFocusable(true);
            headersText.setFocusableInTouchMode(true);
            headersText.setClickable(true);
            
            requestBodyText.setFocusable(true);
            requestBodyText.setFocusableInTouchMode(true);
            requestBodyText.setClickable(true);
            
            editButton.setText("Save");
        } else {
            // 退出编辑模式
            urlText.setFocusable(false);
            urlText.setFocusableInTouchMode(false);
            urlText.setClickable(false);
            
            headersText.setFocusable(false);
            headersText.setFocusableInTouchMode(false);
            headersText.setClickable(false);
            
            requestBodyText.setFocusable(false);
            requestBodyText.setFocusableInTouchMode(false);
            requestBodyText.setClickable(false);
            
            editButton.setText("Edit");
            
            // 更新当前日志
            currentLog.setUrl(urlText.getText().toString());
            currentLog.setHeaders(headersText.getText().toString());
            currentLog.setRequestBody(requestBodyText.getText().toString());
        }
    }

    private void resendRequest() {
        // 禁用按钮防止重复点击
        resendButton.setEnabled(false);
        
        // 显示加载状态
        responseBodyText.setText("Sending request...");
        errorContainer.setVisibility(View.GONE);
        
        // 创建新的请求
        String url = urlText.getText().toString();
        String method = currentLog.getMethod();
        String body = requestBodyText.getText().toString();
        
        // 构建请求
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        
        
        // 添加请求体（仅非GET请求）
        if (!method.equals("GET") && !body.isEmpty()) {
            requestBuilder.method(method, RequestBody.create(MediaType.parse("application/json"), body));
        }
        
        Request request = requestBuilder.build();
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        // 发送请求
        new Thread(() -> {
            try {
                // 发送请求并获取响应
                Response response = client.newCall(request).execute();
                
                // 记录请求结束时间
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                // 读取响应信息
                final int statusCode = response.code();
                final long finalResponseTime = responseTime;
                final String finalBody = body;
                String tempResponseBody = "";
                if (response.body() != null) {
                    tempResponseBody = response.body().string();
                }
                final String finalResponseBody = tempResponseBody;
                
                // 处理响应
                runOnUiThread(() -> {
                    // 创建新的NetworkLog对象
                    NetworkLog newLog = new NetworkLog(url, method);
                    newLog.setStatusCode(statusCode);
                    newLog.setResponseTime(finalResponseTime);
                    newLog.setRequestBody(finalBody);
                    newLog.setResponseBody(finalResponseBody);
                    newLog.setHeaders(currentLog.getHeaders());
                    
                    // 更新当前日志
                    currentLog = newLog;
                    
                    // 更新界面显示
                    displayLogDetails(currentLog);
                    
                    // 恢复按钮状态
                    resendButton.setEnabled(true);
                });
            } catch (IOException e) {
                // 记录请求结束时间
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                
                final long finalResponseTime = responseTime;
                final String finalBody = body;
                final String errorMessage = e.getMessage();
                
                runOnUiThread(() -> {
                    // 创建新的NetworkLog对象记录错误
                    NetworkLog newLog = new NetworkLog(url, method);
                    newLog.setStatusCode(0);
                    newLog.setResponseTime(finalResponseTime);
                    newLog.setRequestBody(finalBody);
                    newLog.setResponseBody("Request failed: " + errorMessage);
                    newLog.setErrorMessage(errorMessage);
                    newLog.setHeaders(currentLog.getHeaders());
                    
                    // 更新当前日志
                    currentLog = newLog;
                    
                    // 更新界面显示
                    displayLogDetails(currentLog);
                    
                    // 恢复按钮状态
                    resendButton.setEnabled(true);
                });
            }
        }).start();
    }
}
