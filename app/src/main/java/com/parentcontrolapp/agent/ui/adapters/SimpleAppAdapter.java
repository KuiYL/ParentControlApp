package com.parentcontrolapp.agent.ui.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.utils.TimeUtils;

/**
 * Адаптер для списка топ приложений
 */
public class SimpleAppAdapter extends ListAdapter<AppUsageItem, SimpleAppAdapter.ViewHolder> {

    public SimpleAppAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage_simple, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppUsageItem item = getItem(position);
        holder.tvRank.setText("#" + (position + 1));
        holder.tvName.setText(item.appName);
        holder.tvTime.setText(TimeUtils.formatMinutes(item.timeMinutes));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_app_rank);
            tvName = itemView.findViewById(R.id.tv_app_name);
            tvTime = itemView.findViewById(R.id.tv_app_time);
        }
    }

    private static final DiffUtil.ItemCallback<AppUsageItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppUsageItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppUsageItem oldItem, @NonNull AppUsageItem newItem) {
                    return oldItem.appName != null && oldItem.appName.equals(newItem.appName);
                }

                @Override
                public boolean areContentsTheSame(@NonNull AppUsageItem oldItem, @NonNull AppUsageItem newItem) {
                    return oldItem.timeMinutes == newItem.timeMinutes;
                }
            };
}