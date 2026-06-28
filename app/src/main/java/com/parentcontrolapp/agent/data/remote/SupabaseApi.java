package com.parentcontrolapp.agent.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.parentcontrolapp.agent.data.model.*;

import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SupabaseApi {

    private static final String TAG = "SupabaseApi";

    private static final String BASE_URL = "https://fxxnxjqywemrmmiztxoq.supabase.co";
    private static final String ANON_KEY = "sb_publishable_wehvh_IhKyD7GuOdjGb2NQ_kUt8UHI7";

    private static final boolean DEBUG = true;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson;

    private static SupabaseApi instance;
    private static Context appContext;

    private SupabaseApi() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .protocols(List.of(Protocol.HTTP_1_1))
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("apikey", ANON_KEY)
                            .header("Authorization", "Bearer " + getStoredToken())
                            .header("Content-Type", "application/json");

                    if (original.body() != null) {
                        builder.header("Prefer", "return=representation");
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        this.gson = new Gson();
    }

    public static synchronized SupabaseApi getInstance(Context context) {
        if (instance == null) {
            appContext = context.getApplicationContext();
            instance = new SupabaseApi();
        }
        return instance;
    }

    private static String storedToken = null;

    public static void setStoredToken(String token) {
        storedToken = token;
    }

    private static String getStoredToken() {
        if (storedToken != null && !storedToken.isEmpty()) {
            return storedToken;
        }

        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String savedToken = prefs.getString("access_token", null);
            if (savedToken != null) {
                storedToken = savedToken;
                return savedToken;
            }
        }

        return ANON_KEY;
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга JSON", e);
            return null;
        }
    }

    private void logRequest(Request request, String tag) {
        if (DEBUG) {
            Log.d(TAG, tag + ": " + request.method() + " " + request.url());
            if (request.body() != null) {
                try {
                    String body = request.body().toString();
                    Log.d(TAG, "Body: " + (body.length() < 500 ? body : body.substring(0, 500) + "..."));
                } catch (Exception e) {
                    Log.d(TAG, "Body: <cannot log>");
                }
            }
        }
    }

    private String logResponse(Response response, String tag) throws IOException {
        String body = "";
        if (response.body() != null) {
            ResponseBody responseBody = response.body();
            body = responseBody.string();
        }

        if (DEBUG) {
            Log.d(TAG, tag + ": HTTP " + response.code() + " " + response.message());
            Log.d(TAG, "Body: " + (body.length() < 500 ? body : body.substring(0, 500) + "..."));
        }

        return body;
    }

    /**
     * Регистрация нового пользователя
     */
    public void signUp(String email, String password, String fullName, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        JsonObject userMetadata = new JsonObject();
        if (fullName != null && !fullName.isEmpty()) {
            userMetadata.addProperty("full_name", fullName);
        }
        body.add("data", userMetadata);

        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/v1/signup")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        logRequest(request, "signUp");
        executeAuthRequest(request, callback);
    }

    /**
     * Вход пользователя
     */
    public void signIn(String email, String password, AuthCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/v1/token?grant_type=password")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        logRequest(request, "signIn");
        executeAuthRequest(request, callback);
    }

    /**
     * Выход из аккаунта
     */
    public void signOut(SimpleCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/v1/logout")
                .post(RequestBody.create("", JSON))
                .build();

        logRequest(request, "signOut");
        executeSimpleRequest(request, callback);
    }

    /**
     * Запрос на восстановление пароля
     * Отправляет письмо со ссылкой на сброс пароля
     *
     * @param email    Email пользователя
     * @param callback Результат операции
     */
    public void recoverPassword(String email, SimpleCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/v1/recover")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        logRequest(request, "recoverPassword");

        executeSimpleRequest(request, callback);
    }

    /**
     * Получить профиль пользователя по ID
     *
     * @param userId   UUID пользователя
     * @param callback Результат с объектом Profile
     */
    public void getProfile(String userId, ProfileCallback callback) {

        String url = BASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=*";

        Request request = new Request.Builder().url(url).get().build();

        logRequest(request, "getProfile");

        executeJsonArrayRequest(request, Profile.class, new JsonArrayCallback<Profile>() {
            @Override
            public void onSuccess(List<Profile> profiles) {
                if (profiles != null && !profiles.isEmpty()) {
                    callback.onSuccess(profiles.get(0));
                } else {
                    callback.onError("Профиль не найден");
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Получить список устройств родителя
     *
     * @param parentId UUID родителя из auth.users
     * @param callback Результат со списком устройств
     */
    public void getDevices(String parentId, DeviceListCallback callback) {
        if (parentId == null || parentId.isEmpty()) {
            callback.onError("ID родителя не указан");
            return;
        }

        HttpUrl url = HttpUrl.parse(BASE_URL + "/rest/v1/devices")
                .newBuilder()
                .addQueryParameter("parent_id", "eq." + parentId)
                .addQueryParameter("order", "created_at.desc")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        logRequest(request, "getDevices");
        executeJsonArrayRequest(request, Device.class, new JsonArrayCallback<Device>() {
            @Override
            public void onSuccess(List<Device> devices) {
                callback.onSuccess(devices);
            }

            @Override
            public void onError(String message) {
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
    public void getDeviceRules(String deviceId, RulesCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/device_rules?device_id=eq." + deviceId + "&select=*")
                .get()
                .build();

        logRequest(request, "getDeviceRules");
        executeJsonArrayRequest(request, DeviceRules.class, new JsonArrayCallback<DeviceRules>() {
            @Override
            public void onSuccess(List<DeviceRules> rulesList) {
                if (rulesList != null && !rulesList.isEmpty()) {
                    callback.onSuccess(rulesList.get(0));
                } else {
                    callback.onError("Правила не найдены");
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Отвязать устройство от родителя (обнулить parent_id)
     */
    public void unlinkDevice(String deviceId, SimpleCallback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError("ID устройства не указан");
            return;
        }

        JsonObject updates = new JsonObject();
        updates.addProperty("parent_id", (String) null);
        updates.addProperty("updated_at", new java.util.Date().toString());

        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/devices?id=eq." + deviceId)
                .patch(RequestBody.create(updates.toString(), JSON))
                .build();

        logRequest(request, "unlinkDevice");
        executeSimpleRequest(request, callback);
    }

    /**
     * Создать правила с дефолтными значениями для нового устройства
     *
     * @param deviceId UUID устройства
     * @param callback Результат операции
     */
    public void createDeviceRules(String deviceId, SimpleCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("device_id", deviceId);

        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/device_rules")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        logRequest(request, "createDeviceRules");
        executeSimpleRequest(request, callback);
    }

    /**
     * Обновить настройки устройства
     *
     * @param deviceId UUID устройства
     * @param updates  JsonObject с полями для обновления
     * @param callback Результат операции
     */
    public void updateDeviceRules(String deviceId, JsonObject updates, SimpleCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/device_rules?device_id=eq." + deviceId)
                .patch(RequestBody.create(updates.toString(), JSON))
                .build();

        logRequest(request, "updateDeviceRules");
        executeSimpleRequest(request, callback);
    }

    public void getActivityLogsWithPagination(String deviceId, String startDate, String endDate,
                                              String eventTypeFilter, String searchQuery,
                                              int limit, int offset, ActivityListCallback callback) {
        String startIso = startDate + "T00:00:00Z";
        String endIso = endDate + "T23:59:59Z";

        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/rest/v1/activity_logs")
                .newBuilder()
                .addQueryParameter("select", "id,device_id,event_type,app_name,url,screenshot_url,duration_seconds,occurred_at,is_blocked,block_reason,risk_score")
                .addQueryParameter("device_id", "eq." + deviceId)
                .addQueryParameter("occurred_at", "gte." + startIso)
                .addQueryParameter("occurred_at", "lte." + endIso)
                .addQueryParameter("order", "occurred_at.desc")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", String.valueOf(offset));

        if (eventTypeFilter != null && !eventTypeFilter.equals("all")) {
            if (eventTypeFilter.equals("apps")) {
                urlBuilder.addQueryParameter("event_type", "in.(app_launch,app_close,window_change)");
            } else if (eventTypeFilter.equals("web")) {
                urlBuilder.addQueryParameter("event_type", "eq.web_visit");
            } else if (eventTypeFilter.equals("blocked")) {
                urlBuilder.addQueryParameter("event_type", "in.(block_triggered,limit_reached)");
            } else if (eventTypeFilter.equals("screenshot")) {
                urlBuilder.addQueryParameter("event_type", "eq.screenshot");
            }
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String likeVal = "*" + searchQuery.trim() + "*";
            urlBuilder.addQueryParameter("or", "(app_name.ilike." + likeVal + ",url.ilike." + likeVal + ")");
        }

        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        logRequest(request, "getActivityLogsWithPagination");

        executeJsonArrayRequest(request, ActivityLog.class, new JsonArrayCallback<ActivityLog>() {
            @Override
            public void onSuccess(List<ActivityLog> logs) {
                callback.onSuccess(logs);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Получить публичную ссылку на скриншот
     *
     * @param deviceId UUID устройства
     * @param fileName Имя файла
     * @return Публичная ссылка для загрузки через Glide/Picasso
     */
    public String getScreenshotUrl(String deviceId, String fileName) {
        return BASE_URL + "/storage/v1/object/public/screenshots/" + deviceId + "/" + fileName;
    }

    private void executeAuthRequest(Request request, AuthCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Auth request failed", e);
                callback.onError("Сетевая ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = logResponse(response, "auth");

                if (response.isSuccessful()) {
                    try {
                        JsonObject json = parseJson(body, JsonObject.class);
                        if (json != null) {
                            if (json.has("access_token")) {
                                setStoredToken(json.get("access_token").getAsString());
                            }
                            callback.onSuccess(json);
                        } else {
                            callback.onError("Пустой ответ от сервера");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Auth parse error", e);
                        callback.onError("Ошибка обработки ответа");
                    }
                } else {
                    try {
                        JsonObject error = parseJson(body, JsonObject.class);
                        String msg = "Ошибка " + response.code();
                        if (error != null) {
                            if (error.has("msg")) {
                                msg = error.get("msg").getAsString();
                            } else if (error.has("error_description")) {
                                msg = error.get("error_description").getAsString();
                            }
                        }
                        callback.onError(msg);
                    } catch (Exception e) {
                        callback.onError("HTTP " + response.code() + ": " + body);
                    }
                }
            }
        });
    }

    private void executeSimpleRequest(Request request, SimpleCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Сетевая ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = "";
                try {
                    if (response.body() != null) body = response.body().string();
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + body);
                    }
                } catch (IOException e) {
                    callback.onError("Ошибка сети: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Привязать устройство по коду (вызов серверной функции claim_device)
     *
     * @param pairingCode Код вида "PC-1234"
     * @param parentEmail Email родителя для привязки
     * @param callback    Результат операции
     */
    public void claimDevice(String pairingCode, String parentEmail, SimpleCallback callback) {
        if (pairingCode == null || pairingCode.isEmpty()) {
            callback.onError("Код привязки не указан");
            return;
        }
        if (parentEmail == null || parentEmail.isEmpty()) {
            callback.onError("Email родителя не указан");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("input_code", pairingCode);
        body.addProperty("parent_email", parentEmail);

        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/rpc/claim_device")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        logRequest(request, "claimDevice");

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "claimDevice network error: " + e.getMessage());
                callback.onError("Сетевая ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "claimDevice response: HTTP " + response.code() + " - " + responseBody);

                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "HTTP " + response.code();
                    try {
                        JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                        if (error.has("message")) {
                            errorMsg = error.get("message").getAsString();
                        }
                    } catch (Exception e) {
                    }
                    Log.e(TAG, "claimDevice error: " + errorMsg);
                    callback.onError(errorMsg);
                }
            }
        });
    }

    /**
     * Получить одно устройство по deviceId
     *
     * @param deviceId uuid
     * @param callback Результат с объектом устройства
     */
    public void getDeviceByCode(String deviceId, DeviceObjectCallback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError("Код устройства не указан");
            return;
        }

        HttpUrl url = HttpUrl.parse(BASE_URL + "/rest/v1/devices")
                .newBuilder()
                .addQueryParameter("id", "eq." + deviceId)
                .addQueryParameter("select", "*,parent:profiles!devices_parent_id_fkey(full_name)")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        logRequest(request, "getDeviceByCode");

        executeJsonArrayRequest(request, Device.class, new JsonArrayCallback<Device>() {
            @Override
            public void onSuccess(List<Device> devices) {
                if (devices != null && !devices.isEmpty()) {
                    callback.onSuccess(devices.get(0));
                } else {
                    callback.onError("Устройство не найдено");
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    /**
     * Обновить устройство по deviceId (для блокировки/разблокировки)
     *
     * @param deviceId uuid
     * @param updates  Поля для обновления
     * @param callback Результат операции
     */
    public void updateDeviceByCode(String deviceId, JsonObject updates, SimpleCallback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError("Код устройства не указан");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/devices?id=eq." + deviceId)
                .patch(RequestBody.create(updates.toString(), JSON))
                .build();

        logRequest(request, "updateDeviceByCode");
        executeSimpleRequest(request, callback);
    }

    /**
     * Выполнение запроса с парсингом массива объектов
     * Используется для эндпоинтов, которые возвращают [ {...}, {...} ]
     */
    private <T> void executeJsonArrayRequest(Request request, Class<T> itemClass, JsonArrayCallback<T> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                callback.onError("Сетевая ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                String body = "";
                try {
                    if (response.body() != null) {
                        body = response.body().string();
                    }

                    if (DEBUG) {
                        Log.d(TAG, "jsonArray: HTTP " + response.code() + " " + response.message());
                        Log.d(TAG, "Body: " + (body.length() < 500 ? body : body.substring(0, 500) + "..."));
                    }

                    if (response.isSuccessful()) {
                        try {
                            List<T> list = new ArrayList<>();
                            if (!body.isEmpty() && !body.equals("[]")) {
                                JsonArray array = gson.fromJson(body, JsonArray.class);
                                if (array != null) {
                                    for (JsonElement element : array) {
                                        T item = gson.fromJson(element, itemClass);
                                        if (item != null) list.add(item);
                                    }
                                }
                            }
                            callback.onSuccess(list);
                        } catch (Exception e) {
                            Log.e(TAG, "Array parse error", e);
                            callback.onError("Ошибка обработки данных: " + e.getMessage());
                        }
                    } else {
                        callback.onError("HTTP " + response.code() + ": " + body);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Response read error", e);
                    callback.onError("Ошибка сети: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Получить использованное время за день через SQL-запрос
     */
    public void getUsedTimeToday(String deviceId, String date, UsedTimeCallback callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/rest/v1/activity_logs")
                .newBuilder()
                .addQueryParameter("device_id", "eq." + deviceId)
                .addQueryParameter("event_type", "in.(app_launch,app_close,window_change,web_visit)")
                .addQueryParameter("occurred_at", "gte." + date + "T00:00:00Z")
                .addQueryParameter("occurred_at", "lte." + date + "T23:59:59Z")
                .addQueryParameter("select", "duration_seconds")
                .build();

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Сетевая ошибка: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        JsonArray array = gson.fromJson(body, JsonArray.class);

                        int totalSeconds = 0;
                        if (array != null) {
                            for (JsonElement element : array) {
                                JsonObject obj = element.getAsJsonObject();
                                if (obj.has("duration_seconds") && !obj.get("duration_seconds").isJsonNull()) {
                                    totalSeconds += obj.get("duration_seconds").getAsInt();
                                }
                            }
                        }
                        int minutes = totalSeconds / 60;
                        callback.onSuccess(minutes);
                    } else {
                        callback.onError("HTTP " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("Ошибка парсинга: " + e.getMessage());
                }
            }
        });
    }

    public interface UsedTimeCallback {
        void onSuccess(int minutes);

        void onError(String message);
    }

    /**
     * Колбэк для получения профиля
     */
    public interface ProfileCallback {
        void onSuccess(Profile profile);

        void onError(String message);
    }

    /**
     * Внутренний интерфейс для парсинга массивов
     */
    private interface JsonArrayCallback<T> {
        void onSuccess(List<T> items);

        void onError(String message);
    }

    public interface AuthCallback {
        void onSuccess(JsonObject result);

        void onError(String message);
    }

    public interface UserCallback {
        void onSuccess(User user);

        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String message);
    }

    /**
     * Колбэк для списка устройств
     */
    public interface DeviceListCallback {
        void onSuccess(List<Device> devices);

        void onError(String message);
    }

    /**
     * Колбэк для получения одного устройства
     */
    public interface DeviceObjectCallback {
        void onSuccess(Device device);

        void onError(String message);
    }

    /**
     * Колбэк для правил устройства
     */
    public interface RulesCallback {
        void onSuccess(DeviceRules rules);

        void onError(String message);
    }

    /**
     * Колбэк для списка логов активности
     */
    public interface ActivityListCallback {
        void onSuccess(List<ActivityLog> logs);

        void onError(String message);
    }
}