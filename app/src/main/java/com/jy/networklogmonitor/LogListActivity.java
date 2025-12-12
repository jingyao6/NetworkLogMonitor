package com.jy.networklogmonitor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogListActivity extends AppCompatActivity {
    private RecyclerView logRecyclerView;
    private LogListAdapter logListAdapter;
    private CopyOnWriteArrayList<NetworkLog> logList;
    private List<NetworkLog> filteredLogList;
    private Button clearLogsButton;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_list);

        // 初始化视图
        logRecyclerView = findViewById(R.id.log_recycler_view);
        clearLogsButton = findViewById(R.id.clear_logs_button);
        searchEditText = findViewById(R.id.search_edit_text);

        // 获取日志列表
        logList = NetworkLogInterceptor.getLogList();
        filteredLogList = new ArrayList<>(logList);

        // 初始化适配器
        logListAdapter = new LogListAdapter(this, filteredLogList);
        logRecyclerView.setAdapter(logListAdapter);

        // 设置清除日志按钮点击事件
        clearLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkLogInterceptor.clearLogs();
                filterLogs("", logList);
            }
        });

        // 设置搜索文本变化监听器
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 不处理
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 搜索文本变化时，过滤日志列表
                filterLogs(s.toString(), logList);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 不处理
            }
        });
    }

    private void filterLogs(String searchText, List<NetworkLog> originalList) {
        filteredLogList.clear();
        
        if (searchText.isEmpty()) {
            // 如果搜索文本为空，显示所有日志
            filteredLogList.addAll(originalList);
        } else {
            // 否则，过滤出URL包含搜索文本的日志
            for (NetworkLog log : originalList) {
                if (log.getUrl().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredLogList.add(log);
                }
            }
        }
        
        // 刷新适配器
        logListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新日志列表
        if (logListAdapter != null) {
            filterLogs(searchEditText.getText().toString(), logList);
        }
    }
}
