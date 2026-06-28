package com.parentcontrolapp.agent.ui.adapters;

public class DeviceItem {
    public String id;
    public String childName;
    public String deviceName;
    public String deviceType;
    public boolean isActive;
    public boolean isOnline;
    public boolean isBlocked;

    public DeviceItem(String id, String childName, String deviceName,
                      String deviceType, boolean isActive, boolean isOnline, boolean isBlocked) {
        this.id = id;
        this.childName = childName;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.isActive = isActive;
        this.isOnline = isOnline;
        this.isBlocked = isBlocked;
    }

    public String getId() { return id; }
    public String getChildName() { return childName; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public boolean isActive() { return isActive; }
    public boolean isOnline() { return isOnline; }
    public boolean isBlocked() { return isBlocked; }

    public String getStatusText() {
        if (isBlocked) return "Заблокировано";
        if (!isActive) return "Неактивно";
        return isOnline ? "Онлайн" : "Офлайн";
    }

    public int getStatusColor() {
        if (isBlocked) return 0xFFE53935;
        if (!isActive) return 0xFF9E9E9E;
        return isOnline ? 0xFF4CAF50 : 0xFF9E9E9E;
    }
}