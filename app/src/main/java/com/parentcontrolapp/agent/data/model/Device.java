package com.parentcontrolapp.agent.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class Device {

    @SerializedName("id")
    public String id;

    @SerializedName("parent_id")
    public String parentId;

    @SerializedName("child_name")
    public String childName;

    @SerializedName("device_name")
    public String deviceName;

    @SerializedName("device_type")
    public String deviceType;

    @SerializedName("device_code")
    public String deviceCode;

    @SerializedName("is_active")
    public boolean isActive;

    @SerializedName("is_blocked")
    public boolean isBlocked;

    @SerializedName("last_seen_at")
    public String lastSeenAt;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    @SerializedName("machine_id")
    public String machineId;

    @SerializedName("os_info")
    public Map<String, Object> osInfo = new HashMap<>();

    public Device() {
    }

    public String getId() { return id; }
    public String getParentId() { return parentId; }
    public String getChildName() { return childName; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public String getDeviceCode() { return deviceCode; }
    public boolean isActive() { return isActive; }
    public boolean isBlocked() { return isBlocked; }
    public String getLastSeenAt() { return lastSeenAt; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getMachineId() { return machineId; }
    public Map<String, Object> getOsInfo() { return osInfo; }

    public boolean isOnline() {
        Long timestamp = parseIsoTimestamp(lastSeenAt);
        if (timestamp == null) return false;
        return System.currentTimeMillis() - timestamp < 5 * 60 * 1000;
    }

    public String getStatusText() {
        if (isBlocked) return "Заблокировано";
        if (!isActive) return "Неактивно";
        return isOnline() ? "Онлайн" : "Офлайн";
    }

    public String getLastSeenFormatted() {
        if (lastSeenAt == null || lastSeenAt.isEmpty()) return "Неизвестно";

        Long timestamp = parseIsoTimestamp(lastSeenAt);
        if (timestamp == null) return lastSeenAt;

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60 * 1000) return "Только что";
        if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + " мин назад";
        if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + " ч назад";
        return (diff / (24 * 60 * 60 * 1000)) + " дн назад";
    }

    private Long parseIsoTimestamp(String isoString) {
        if (isoString == null || isoString.isEmpty()) return null;

        try {
            String cleaned = isoString
                    .replace("Z", "+0000")
                    .replaceAll("\\.\\d{3}\\+", "+")
                    .replaceAll("\\.\\d{6}\\+", "+");

            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault());
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            java.util.Date date = format.parse(cleaned);
            return date != null ? date.getTime() : null;
        } catch (Exception e) {
            try {
                java.text.SimpleDateFormat altFormat = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                altFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = altFormat.parse(isoString);
                return date != null ? date.getTime() : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}