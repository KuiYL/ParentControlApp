package com.parentcontrolapp.agent.data.model;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ActivityLog {
    @SerializedName("id")
    public String id;

    @SerializedName("device_id")
    public String deviceId;

    @SerializedName("event_type")
    public String eventType;

    @SerializedName("app_name")
    public String appName;

    @SerializedName("url")
    public String url;

    @SerializedName("screenshot_url")
    public String screenshotUrl;

    @SerializedName("duration_seconds")
    public int durationSeconds;

    @SerializedName("occurred_at")
    public String occurredAt;

    @SerializedName("is_blocked")
    public boolean isBlocked;

    @SerializedName("block_reason")
    public String blockReason;

    @SerializedName("risk_score")
    public int riskScore;

    @SerializedName("analysis")
    public String analysis;

    public String getFormattedTime() {
        if (occurredAt == null || occurredAt.isEmpty()) return "";

        try {
            String cleaned = occurredAt.replace("Z", "+0000").replaceAll("\\.\\d+\\+", "+");

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(cleaned);

            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm • dd.MM", Locale.getDefault());
                outputFormat.setTimeZone(TimeZone.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            return occurredAt;
        }
        return occurredAt;
    }

    public long getTimestampMillis() {
        if (occurredAt == null || occurredAt.isEmpty()) return 0;

        try {
            String cleaned = occurredAt.replace("Z", "+0000").replaceAll("\\.\\d+\\+", "+");

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(cleaned);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getEventType() { return eventType; }
    public String getAppName() { return appName; }
    public String getUrl() { return url; }
    public String getScreenshotUrl() { return screenshotUrl; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getOccurredAt() { return occurredAt; }

    public boolean isBlocked() { return isBlocked; }
    public String getBlockReason() { return blockReason; }
    public int getRiskScore() { return riskScore; }
    public String getAnalysis() { return analysis; }

    public String getEventTitle() {
        if (eventType == null) return "Событие";
        switch (eventType) {
            case "app_launch": return "Запуск";
            case "app_close": return "Закрытие";
            case "web_visit": return "Веб";
            case "block_triggered": return "Блок";
            case "screenshot": return "Скрин";
            case "limit_reached": return "Лимит";
            case "night_mode_start": return "Ночь";
            case "night_mode_end": return "День";
            case "device_online": return "Онлайн";
            case "device_offline": return "Офлайн";
            case "window_change": return "Окно";
            default: return eventType;
        }
    }

    public boolean isBlockEvent() {
        return "block_triggered".equals(eventType) || "limit_reached".equals(eventType);
    }

    public boolean hasScreenshot() {
        return screenshotUrl != null && !screenshotUrl.isEmpty();
    }

    public String getDescription() {
        if (appName != null && !appName.isEmpty()) return appName;
        if (url != null && !url.isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                return uri.getHost() != null ? uri.getHost() : url;
            } catch (Exception e) {
                return url;
            }
        }
        return getEventTitle();
    }
}