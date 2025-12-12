package com.jy.networklogmonitor;

import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogDetailActivity extends AppCompatActivity {
    private TextView methodText;
    private TextView statusCodeText;
    private TextView responseTimeText;
    private TextView urlText;
    private TextView timestampText;
    private TextView headersText;
    private TextView requestBodyText;
    private TextView responseBodyText;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

        // 获取日志位置
        int logPosition = getIntent().getIntExtra("log_position", -1);
        if (logPosition == -1) {
            finish();
            return;
        }

        // 获取日志列表并显示详情
        CopyOnWriteArrayList<NetworkLog> logList = NetworkLogInterceptor.getLogList();
        if (logPosition < logList.size()) {
            NetworkLog log = logList.get(logPosition);
            displayLogDetails(log);
        }
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

        // 根据状态码设置不同颜色
        if (log.getStatusCode() >= 200 && log.getStatusCode() < 300) {
            statusCodeText.setBackgroundResource(R.drawable.status_success_background);
        } else if (log.getStatusCode() >= 300 && log.getStatusCode() < 400) {
            statusCodeText.setBackgroundResource(R.drawable.status_redirect_background);
        } else if (log.getStatusCode() >= 400 && log.getStatusCode() < 500) {
            statusCodeText.setBackgroundResource(R.drawable.status_client_error_background);
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
}
