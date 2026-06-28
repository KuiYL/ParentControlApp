package com.parentcontrolapp.agent.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parentcontrolapp.agent.data.model.ActivityLog;
import com.parentcontrolapp.agent.data.model.DeviceRules;
import com.parentcontrolapp.agent.data.model.Profile;

import java.lang.reflect.Type;
import java.util.List;

public class DataCacheManager {
    private static final String PREF_NAME = "data_cache";
    private static final long CACHE_DURATION_5MIN = 5 * 60 * 1000;
    private static final long CACHE_DURATION_10MIN = 10 * 60 * 1000;
    private final SharedPreferences prefs;
    private final Gson gson;

    public DataCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // Кэш для ActivityLogs
    public void saveActivityLogs(String deviceId, List<ActivityLog> logs) {
        String key = "logs_" + deviceId;
        Type type = new TypeToken<List<ActivityLog>>() {
        }.getType();
        prefs.edit()
                .putString(key, gson.toJson(logs, type))
                .putLong(key + "_time", System.currentTimeMillis())
                .apply();
    }

    public List<ActivityLog> getActivityLogs(String deviceId) {
        String key = "logs_" + deviceId;
        long timestamp = prefs.getLong(key + "_time", 0);

        if (System.currentTimeMillis() - timestamp > CACHE_DURATION_5MIN) {
            return null;
        }

        String json = prefs.getString(key, null);
        if (json == null) return null;

        try {
            Type type = new TypeToken<List<ActivityLog>>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveProfile(Profile profile) {
        String key = "profile_" + profile.getId();
        prefs.edit()
                .putString(key, gson.toJson(profile))
                .putLong(key + "_time", System.currentTimeMillis())
                .apply();
    }

    public Profile getProfile(String userId) {
        String key = "profile_" + userId;
        long timestamp = prefs.getLong(key + "_time", 0);

        if (System.currentTimeMillis() - timestamp > CACHE_DURATION_10MIN) {
            return null;
        }

        String json = prefs.getString(key, null);
        return json != null ? gson.fromJson(json, Profile.class) : null;
    }

    public void clearProfile(String userId) {
        prefs.edit().remove("profile_" + userId).apply();
    }

    // Очистка всего кэша
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}