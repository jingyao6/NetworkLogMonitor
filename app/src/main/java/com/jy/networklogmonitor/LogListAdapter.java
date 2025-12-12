package com.jy.networklogmonitor;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder> {
    private final Context context;
    private final List<NetworkLog> logList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LogListAdapter(Context context, List<NetworkLog> logList) {
        this.context = context;
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.log_item, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NetworkLog log = logList.get(position);

        holder.methodText.setText(log.getMethod());
        holder.statusCodeText.setText(String.valueOf(log.getStatusCode()));
        holder.responseTimeText.setText(log.getResponseTime() + "ms");
        holder.urlText.setText(log.getUrl());
        holder.timestampText.setText(dateFormat.format(log.getTimestamp()));

        // 根据状态码设置不同颜色
        if (log.getStatusCode() >= 200 && log.getStatusCode() < 300) {
            holder.statusCodeText.setBackgroundResource(R.drawable.status_success_background);
        } else if (log.getStatusCode() >= 300 && log.getStatusCode() < 400) {
            holder.statusCodeText.setBackgroundResource(R.drawable.status_redirect_background);
        } else if (log.getStatusCode() >= 400 && log.getStatusCode() < 500) {
            holder.statusCodeText.setBackgroundResource(R.drawable.status_client_error_background);
        } else {
            holder.statusCodeText.setBackgroundResource(R.drawable.status_server_error_background);
        }

        // 根据请求方法设置不同颜色
        switch (log.getMethod()) {
            case "GET":
                holder.methodText.setBackgroundResource(R.drawable.method_get_background);
                break;
            case "POST":
                holder.methodText.setBackgroundResource(R.drawable.method_post_background);
                break;
            case "PUT":
                holder.methodText.setBackgroundResource(R.drawable.method_put_background);
                break;
            case "DELETE":
                holder.methodText.setBackgroundResource(R.drawable.method_delete_background);
                break;
            default:
                holder.methodText.setBackgroundResource(R.drawable.method_background);
                break;
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            // 查找原始列表中的真实位置
            int originalPosition = NetworkLogInterceptor.getLogList().indexOf(log);
            
            Intent intent = new Intent(context, LogDetailActivity.class);
            intent.putExtra("log_position", originalPosition);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView methodText;
        TextView statusCodeText;
        TextView responseTimeText;
        TextView urlText;
        TextView timestampText;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            methodText = itemView.findViewById(R.id.method_text);
            statusCodeText = itemView.findViewById(R.id.status_code_text);
            responseTimeText = itemView.findViewById(R.id.response_time_text);
            urlText = itemView.findViewById(R.id.url_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
        }
    }
}
