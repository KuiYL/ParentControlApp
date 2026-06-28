package com.parentcontrolapp.agent.ui.adapters;

/**
 * Модель для статистики использования приложения
 */
public class AppUsageItem {
    public final String appName;
    public final int timeMinutes;

    public AppUsageItem(String appName, int timeMinutes) {
        this.appName = appName;
        this.timeMinutes = timeMinutes;
    }

    public String getAppName() {
        return appName;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }
}