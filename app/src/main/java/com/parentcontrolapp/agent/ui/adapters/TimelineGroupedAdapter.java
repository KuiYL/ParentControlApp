package com.parentcontrolapp.agent.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.ActivityLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimelineGroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private List<TimelineItem> items;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnBlockItemListener {
        void onBlockItemClick(ActivityLog log);
    }

    private OnBlockItemListener blockListener;

    public TimelineGroupedAdapter(OnBlockItemListener blockListener) {
        this.items = new ArrayList<>();
        this.blockListener = blockListener;
        timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    public TimelineGroupedAdapter() {
        this.items = new ArrayList<>();
        timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    public TimelineGroupedAdapter(List<TimelineItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
    }

    public void updateData(List<TimelineItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type == TimelineItem.Type.DATE_HEADER ? TYPE_HEADER : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log_event, parent, false);
            return new EventViewHolder(view, blockListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimelineItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.dateLabel);
        } else if (holder instanceof EventViewHolder && item.log != null) {
            ((EventViewHolder) holder).bind(item.log, timeFormat);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date_header);
        }

        void bind(String dateLabel) {
            tvDate.setText(dateLabel != null ? dateLabel : "");
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvType, tvDescription, tvStatus, tvExtra;
        LinearLayout containerExtra;
        ImageButton btnBlockItem;

        private OnBlockItemListener blockListener;

        EventViewHolder(@NonNull View itemView, OnBlockItemListener blockListener) {
            super(itemView);
            this.blockListener = blockListener;

            tvTime = itemView.findViewById(R.id.tv_time);
            tvType = itemView.findViewById(R.id.tv_type);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvExtra = itemView.findViewById(R.id.tv_extra);
            containerExtra = itemView.findViewById(R.id.container_extra);
            btnBlockItem = itemView.findViewById(R.id.btn_block_item);
        }

        void bind(ActivityLog log, SimpleDateFormat timeFormatter) {
            if (log == null) return;

            String timeString = "00:00";
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

            tvType.setText(getEventTitle(log.eventType));
            tvDescription.setText(buildDescription(log));

            if ("block_triggered".equals(log.eventType)) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("✕");
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.error));
            } else {
                tvStatus.setVisibility(View.GONE);
            }

            if ("app_launch".equals(log.eventType) || "app_close".equals(log.eventType) || "web_visit".equals(log.eventType)) {
                btnBlockItem.setVisibility(View.VISIBLE);
                btnBlockItem.setOnClickListener(v -> {
                    if (blockListener != null) {
                        blockListener.onBlockItemClick(log);
                    }
                });
            } else {
                btnBlockItem.setVisibility(View.GONE);
            }

            showExtraInfo(log);
        }

        private void showExtraInfo(ActivityLog log) {
            if (log.screenshotUrl != null && !log.screenshotUrl.isEmpty()) {
                containerExtra.setVisibility(View.VISIBLE);
                tvExtra.setText("Скриншот");

                try {
                    com.bumptech.glide.Glide.with(itemView.getContext())
                            .load(log.screenshotUrl)
                            .placeholder(R.drawable.bg_chart_placeholder)
                            .error(R.drawable.bg_chart_placeholder)
                            .centerCrop()
                            .into((android.widget.ImageView) itemView.findViewById(R.id.img_preview));
                } catch (Exception e) {
                    ((android.widget.ImageView) itemView.findViewById(R.id.img_preview))
                            .setImageResource(R.drawable.bg_chart_placeholder);
                }
                return;
            }

            if ("web_visit".equals(log.eventType) && log.url != null && !log.url.isEmpty()) {
                containerExtra.setVisibility(View.VISIBLE);

                String domain = extractDomain(log.url);
                tvExtra.setText(domain);

                ((android.widget.ImageView) itemView.findViewById(R.id.img_preview))
                        .setImageResource(android.R.drawable.ic_menu_search);
                return;
            }

            containerExtra.setVisibility(View.GONE);
        }

        private String extractDomain(String url) {
            if (url == null) return "";
            try {
                java.net.URI uri = java.net.URI.create(url);
                String host = uri.getHost();
                if (host != null) {
                    if (host.startsWith("www.")) {
                        host = host.substring(4);
                    }
                    return host;
                }
                return url;
            } catch (Exception e) {
                return url.length() > 30 ? url.substring(0, 30) + "..." : url;
            }
        }

        private String buildDescription(ActivityLog log) {
            if ("app_launch".equals(log.eventType) || "app_close".equals(log.eventType)) {
                String app = log.appName != null ? log.appName : "Приложение";
                if (log.durationSeconds > 0) {
                    int mins = log.durationSeconds / 60;
                    return app + " • " + mins + " мин";
                }
                return app;
            }
            if ("web_visit".equals(log.eventType)) {
                if (log.url != null && !log.url.isEmpty()) {
                    return truncateUrl(log.url);
                }
                return "Веб-страница";
            }
            if ("block_triggered".equals(log.eventType)) {
                return log.appName != null ? "Заблокировано: " + log.appName : "Контент заблокирован";
            }
            if ("screenshot".equals(log.eventType)) {
                return "Скриншот экрана";
            }
            return log.eventType;
        }

        private String truncateUrl(String url) {
            if (url == null) return "";
            if (url.length() <= 30) return url;
            try {
                java.net.URI uri = java.net.URI.create(url);
                String domain = uri.getHost();
                String path = uri.getPath();
                if (path != null && path.length() > 15) {
                    path = path.substring(0, 15) + "...";
                }
                return domain + (path != null ? path : "");
            } catch (Exception e) {
                return url.substring(0, 30) + "...";
            }
        }

        private String getEventTitle(String eventType) {
            if (eventType == null) return "Событие";
            switch (eventType) {
                case "app_launch":
                    return "Запуск";
                case "app_close":
                    return "Закрытие";
                case "web_visit":
                    return "Веб";
                case "block_triggered":
                    return "Блок";
                case "screenshot":
                    return "Скрин";
                case "limit_reached":
                    return "Лимит";
                case "night_mode_start":
                    return "Ночь";
                case "night_mode_end":
                    return "День";
                case "device_online":
                    return "Онлайн";
                case "device_offline":
                    return "Офлайн";
                default:
                    return eventType;
            }
        }
    }
}