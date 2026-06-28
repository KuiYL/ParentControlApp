package com.parentcontrolapp.agent.ui.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.ActivityLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScreenshotAdapter extends RecyclerView.Adapter<ScreenshotAdapter.ViewHolder> {

    private List<ActivityLog> screenshots;
    private final OnScreenshotClickListener listener;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnScreenshotClickListener {
        void onScreenshotClick(ActivityLog screenshot);
    }

    public ScreenshotAdapter(List<ActivityLog> screenshots, OnScreenshotClickListener listener) {
        this.screenshots = screenshots != null ? screenshots : new ArrayList<>();
        this.listener = listener;
        timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<ActivityLog> newScreenshots) {
        this.screenshots = newScreenshots != null ? newScreenshots : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_screenshots, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLog log = screenshots.get(position);
        holder.bind(log, listener, timeFormat);
    }

    @Override
    public int getItemCount() {
        return screenshots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgScreenshot;
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgScreenshot = itemView.findViewById(R.id.img_screenshot);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(ActivityLog log, OnScreenshotClickListener listener, SimpleDateFormat timeFormatter) {
            String timeString = "--:--";
            if (log.occurredAt != null) {
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    String cleanDate = log.occurredAt.replace("Z", "").split("\\+")[0];
                    if (cleanDate.contains(".")) {
                        cleanDate = cleanDate.substring(0, cleanDate.indexOf("."));
                    }

                    Date date = isoFormat.parse(cleanDate);
                    if (date != null) {
                        timeString = timeFormatter.format(date);
                    }
                } catch (Exception e) {
                }
            }

            tvTime.setText(timeString);

            if (log.screenshotUrl != null && !log.screenshotUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(log.screenshotUrl)
                        .placeholder(R.drawable.bg_chart_placeholder)
                        .error(R.drawable.bg_chart_placeholder)
                        .centerCrop()
                        .into(imgScreenshot);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onScreenshotClick(log);
            });
        }
    }
}