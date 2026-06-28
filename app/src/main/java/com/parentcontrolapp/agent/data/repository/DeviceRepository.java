package com.parentcontrolapp.agent.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.parentcontrolapp.agent.data.DataCacheManager;
import com.parentcontrolapp.agent.data.model.Device;
import com.parentcontrolapp.agent.data.model.DeviceRules;
import com.parentcontrolapp.agent.data.model.ActivityLog;
import com.parentcontrolapp.agent.data.remote.SupabaseApi;

import java.util.List;

public class DeviceRepository {

    private static final String TAG = "DeviceRepository";
    private final SupabaseApi api;
    private Context context;
    private DataCacheManager cache;

    public DeviceRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = SupabaseApi.getInstance(context);
        this.cache = new DataCacheManager(context);
    }

    /**
     * Привязать устройство по коду (вызывается, когда родитель вводит код)
     *
     * @param pairingCode Код вида "PC-1234"
     * @param parentEmail Email родителя
     * @param callback    Результат операции
     */
    public void claimDevice(String pairingCode, String parentEmail, SimpleCallback callback) {
        if (pairingCode == null || pairingCode.isEmpty()) {
            callback.onError("Код привязки не указан");
            return;
        }

        api.claimDevice(pairingCode, parentEmail, new SupabaseApi.SimpleCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "claimDevice error: " + message);

                String errorMsg = message;
                if (message.contains("Invalid or expired")) {
                    errorMsg = "Неверный или истёкший код";
                } else if (message.contains("Parent account not found")) {
                    errorMsg = "Аккаунт не найден";
                }
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * Получить список устройств родителя
     *
     * @param parentId UUID родителя из auth.users
     * @param callback Результат со списком устройств
     */
    public void fetchDevices(String parentId, DeviceListCallback callback) {
        if (parentId == null || parentId.isEmpty()) {
            callback.onError("ID пользователя не указан");
            return;
        }

        api.getDevices(parentId, new SupabaseApi.DeviceListCallback() {
            @Override
            public void onSuccess(List<Device> devices) {
                callback.onSuccess(devices);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchDevices error: " + message);
                callback.onError(message);
            }
        });
    }

    /**
     * Получить устройство по deviceId
     */
    public void fetchDeviceByCode(String deviceId, DeviceObjectCallback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError("Код устройства не указан");
            return;
        }

        api.getDeviceByCode(deviceId, new SupabaseApi.DeviceObjectCallback() {
            @Override
            public void onSuccess(Device device) {
                callback.onSuccess(device);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchDeviceByCode error: " + message);
                callback.onError(message);
            }
        });
    }

    /**
     * Отвязать устройство от родителя
     * Устройство остаётся в БД, но parent_id становится null
     */
    public void unlinkDevice(String deviceId, SimpleCallback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError("ID устройства не указан");
            return;
        }

        api.unlinkDevice(deviceId, new SupabaseApi.SimpleCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "unlinkDevice error: " + message);
                callback.onError(message);
            }
        });
    }

    /**
     * Заблокировать/разблокировать устройство по deviceId
     */
    public void toggleDeviceBlock(String deviceId, boolean blocked, ActionCallback callback) {
        JsonObject updates = new JsonObject();
        updates.addProperty("is_blocked", blocked);
        updates.addProperty("updated_at", new java.util.Date().toString());

        api.updateDeviceByCode(deviceId, updates, new SupabaseApi.SimpleCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "toggleDeviceBlock error: " + message);
                callback.onError(message);
            }
        });
    }

    /**
     * Получить настройки устройства
     *
     * @param deviceId UUID устройства
     * @param callback Результат с объектом правил
     */
    public void fetchDeviceRules(String deviceId, RulesCallback callback) {
        api.getDeviceRules(deviceId, new SupabaseApi.RulesCallback() {
            @Override
            public void onSuccess(DeviceRules rules) {
                callback.onSuccess(rules);
            }

            @Override
            public void onError(String message) {
                if (message.contains("not found") ||
                        message.contains("404") ||
                        message.contains("Правила не найдены") ||
                        message.contains("пустой ответ")) {

                    createDefaultRules(deviceId, new SupabaseApi.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            api.getDeviceRules(deviceId, new SupabaseApi.RulesCallback() {
                                @Override
                                public void onSuccess(DeviceRules rules) {
                                    callback.onSuccess(rules);
                                }
                                @Override
                                public void onError(String msg) {
                                    callback.onError(msg);
                                }
                            });
                        }
                        @Override
                        public void onError(String createMsg) {
                            callback.onError("Не удалось создать правила: " + createMsg);
                        }
                    });
                } else {
                    Log.e("RulesDebug", "Ошибка загрузки правил: " + message);
                    callback.onError(message);
                }
            }
        });
    }


    /**
     * Создать правила с дефолтными значениями
     */
    private void createDefaultRules(String deviceId, SupabaseApi.SimpleCallback callback) {
        api.createDeviceRules(deviceId, callback);
    }

    /**
     * Сохранить настройки устройства
     *
     * @param deviceId UUID устройства
     * @param rules    Объект с новыми настройками
     * @param callback Результат операции
     */
    public void updateDeviceRules(String deviceId, DeviceRules rules, SimpleCallback callback) {
        if (rules == null) {
            callback.onError("Rules are null");
            return;
        }
        JsonObject updates = new JsonObject();

        // Существующие поля
        if (rules.dailyLimitMinutes != null) {
            updates.addProperty("daily_limit_minutes", rules.dailyLimitMinutes);
        }
        if (rules.limitEnabled != null) {
            updates.addProperty("limit_enabled", rules.limitEnabled);
        }
        if (rules.nightModeEnabled != null) {
            updates.addProperty("night_mode_enabled", rules.nightModeEnabled);
        }
        if (rules.nightModeStart != null) {
            updates.addProperty("night_mode_start", rules.nightModeStart);
        }
        if (rules.nightModeEnd != null) {
            updates.addProperty("night_mode_end", rules.nightModeEnd);
        }
        if (rules.blockAdultContent != null) {
            updates.addProperty("block_adult_content", rules.blockAdultContent);
        }
        if (rules.blockSocialNetworks != null) {
            updates.addProperty("block_social_networks", rules.blockSocialNetworks);
        }
        if (rules.blockGames != null) {
            updates.addProperty("block_games", rules.blockGames);
        }

        if (rules.blockedApps != null) {
            JsonArray appsArray = new JsonArray();
            for (String app : rules.blockedApps) {
                if (app != null && !app.trim().isEmpty()) {
                    appsArray.add(app.trim());
                }
            }
            updates.add("blocked_apps", appsArray);
        }

        if (rules.blockedDomains != null) {
            JsonArray domainsArray = new JsonArray();
            for (String domain : rules.blockedDomains) {
                if (domain != null && !domain.trim().isEmpty()) {
                    domainsArray.add(domain.trim());
                }
            }
            updates.add("blocked_domains", domainsArray);
        }

        if (updates.entrySet().isEmpty()) {
            callback.onSuccess();
            return;
        }

        api.updateDeviceRules(deviceId, updates, new SupabaseApi.SimpleCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }
            @Override
            public void onError(String message) {
                Log.e(TAG, "updateDeviceRules error: " + message);
                callback.onError(message);
            }
        });
    }


    public void fetchActivityLogs(String deviceId, String startDate, String endDate,
                                  String eventTypeFilter, String searchQuery,
                                  int limit, int offset, ActivityListCallback callback) {
        if (deviceId == null) {
            callback.onError("device_id не указан");
            return;
        }

        if (offset == 0 && "all".equals(eventTypeFilter) && (searchQuery == null || searchQuery.isEmpty())) {
            List<ActivityLog> cachedLogs = cache.getActivityLogs(deviceId);
            if (cachedLogs != null && !cachedLogs.isEmpty()) {
                Log.d(TAG, "Загружено из кэша: " + cachedLogs.size() + " записей");
                callback.onSuccess(cachedLogs);
                fetchAndCacheLogs(deviceId, startDate, endDate, eventTypeFilter, searchQuery, limit, offset, callback);
                return;
            }
        }

        fetchAndCacheLogs(deviceId, startDate, endDate, eventTypeFilter, searchQuery, limit, offset, callback);
    }

    private void fetchAndCacheLogs(String deviceId, String startDate, String endDate,
                                   String eventTypeFilter, String searchQuery,
                                   int limit, int offset, ActivityListCallback callback) {
        api.getActivityLogsWithPagination(deviceId, startDate, endDate, eventTypeFilter, searchQuery, limit, offset,
                new SupabaseApi.ActivityListCallback() {
                    @Override
                    public void onSuccess(List<ActivityLog> logs) {
                        if (offset == 0 && "all".equals(eventTypeFilter) && (searchQuery == null || searchQuery.isEmpty())) {
                            cache.saveActivityLogs(deviceId, logs);
                        }
                        callback.onSuccess(logs);
                    }
                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    /**
     * Получить использованное время за сегодня из activity_logs
     */
    public void fetchUsedTimeToday(String deviceId, UsedTimeCallback callback) {
        if (deviceId == null) {
            callback.onError("device_id не указан");
            return;
        }

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        api.getUsedTimeToday(deviceId, today, new SupabaseApi.UsedTimeCallback() {
            @Override
            public void onSuccess(int minutes) {
                callback.onSuccess(minutes);
            }
            @Override
            public void onError(String message) {
                Log.w("DeviceRepo", "Не удалось получить used_time: " + message);
                callback.onSuccess(0); // Фолбэк: 0 минут
            }
        });
    }

    public interface UsedTimeCallback {
        void onSuccess(int minutes);
        void onError(String message);
    }

    public interface DeviceListCallback {
        void onSuccess(List<Device> devices);

        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface RulesCallback {
        void onSuccess(DeviceRules rules);

        void onError(String message);
    }

    public interface ActivityListCallback {
        void onSuccess(List<ActivityLog> logs);

        void onError(String message);
    }
    public interface DeviceObjectCallback {
        void onSuccess(Device device);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

}