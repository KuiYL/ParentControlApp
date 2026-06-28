package com.parentcontrolapp.agent.data.model;

import com.google.gson.annotations.SerializedName;

public class DeviceRules {
    @SerializedName("id")
    public String id;

    @SerializedName("device_id")
    public String deviceId;

    @SerializedName("daily_limit_minutes")
    public Integer dailyLimitMinutes;

    @SerializedName("limit_enabled")
    public Boolean limitEnabled;

    @SerializedName("night_mode_enabled")
    public Boolean nightModeEnabled;

    @SerializedName("night_mode_start")
    public String nightModeStart;

    @SerializedName("night_mode_end")
    public String nightModeEnd;

    @SerializedName("block_adult_content")
    public Boolean blockAdultContent;

    @SerializedName("block_social_networks")
    public Boolean blockSocialNetworks;

    @SerializedName("block_games")
    public Boolean blockGames;

    @SerializedName("blocked_apps")
    public String[] blockedApps;

    @SerializedName("blocked_domains")
    public String[] blockedDomains;

    @SerializedName("screenshot_interval_minutes")
    public Integer screenshotIntervalMinutes;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public DeviceRules() {
    }

    public String getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public Integer getDailyLimitMinutes() { return dailyLimitMinutes; }
    public String getNightModeStart() { return nightModeStart; }
    public String getNightModeEnd() { return nightModeEnd; }
    public String[] getBlockedApps() { return blockedApps; }
    public String[] getBlockedDomains() { return blockedDomains; }
    public Integer getScreenshotIntervalMinutes() { return screenshotIntervalMinutes; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public boolean isLimitEnabled() {
        return limitEnabled != null && limitEnabled;
    }

    public boolean isNightModeEnabled() {
        return nightModeEnabled != null && nightModeEnabled;
    }

    public boolean isBlockAdultContent() {
        return blockAdultContent != null && blockAdultContent;
    }

    public boolean isBlockSocialNetworks() {
        return blockSocialNetworks != null && blockSocialNetworks;
    }

    public boolean isBlockGames() {
        return blockGames != null && blockGames;
    }

    public int getBlockedAppsCount() {
        return blockedApps != null ? blockedApps.length : 0;
    }

    public int getBlockedDomainsCount() {
        return blockedDomains != null ? blockedDomains.length : 0;
    }

    public boolean isAppBlocked(String packageName) {
        if (blockedApps == null || packageName == null) return false;
        for (String app : blockedApps) {
            if (packageName.equals(app)) return true;
        }
        return false;
    }

    public boolean isDomainBlocked(String domain) {
        if (blockedDomains == null || domain == null) return false;
        for (String d : blockedDomains) {
            if (domain.contains(d)) return true;
        }
        return false;
    }

    public int getScreenshotInterval() {
        return screenshotIntervalMinutes != null ? screenshotIntervalMinutes : 30;
    }
}