package com.parentcontrolapp.agent.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonObject;
import com.parentcontrolapp.agent.data.DataCacheManager;
import com.parentcontrolapp.agent.data.model.Profile;
import com.parentcontrolapp.agent.data.remote.SupabaseApi;

public class AuthRepository {
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";

    private final SupabaseApi api;
    private final SharedPreferences prefs;
    private final DataCacheManager cache;

    public AuthRepository(Context context) {
        this.api = SupabaseApi.getInstance(context);
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.cache = new DataCacheManager(context);
    }

    public void register(String email, String password, String fullName, AuthCallback callback) {
        api.signUp(email, password, fullName, new SupabaseApi.AuthCallback() {
            @Override
            public void onSuccess(JsonObject result) {
                if (result.has("access_token") && result.has("user")) {
                    try {
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Ошибка обработки ответа");
                    }
                } else {
                    callback.onError("Неполный ответ от сервера");
                }
            }

            @Override
            public void onError(String message) {
                String errorMsg = parseAuthError(message);
                callback.onError(errorMsg);
            }
        });
    }

    public void login(String email, String password, AuthCallback callback) {
        api.signIn(email, password, new SupabaseApi.AuthCallback() {
            @Override
            public void onSuccess(JsonObject result) {
                if (result.has("access_token") && result.has("user")) {
                    try {
                        String token = result.get("access_token").getAsString();
                        String userId = result.getAsJsonObject("user").get("id").getAsString();

                        saveSession(token, userId, email);
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Ошибка обработки ответа");
                    }
                } else {
                    callback.onError("Неполный ответ от сервера");
                }
            }

            @Override
            public void onError(String message) {
                String errorMsg = parseAuthError(message);
                callback.onError(errorMsg);
            }
        });
    }

    public void logout(AuthCallback callback) {
        api.signOut(new SupabaseApi.SimpleCallback() {
            @Override
            public void onSuccess() {
                clearSession();
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                clearSession();
                callback.onSuccess();
            }
        });
    }


    public void getProfile(String userId, ProfileCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("User ID не указан");
            return;
        }

        Profile cachedProfile = cache.getProfile(userId);
        if (cachedProfile != null) {
            callback.onSuccess(cachedProfile);
            refreshProfileInBackground(userId);
            return;
        }
        api.getProfile(userId, new SupabaseApi.ProfileCallback() {
            @Override
            public void onSuccess(Profile profile) {
                cache.saveProfile(profile);
                callback.onSuccess(profile);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void refreshProfileInBackground(String userId) {
        api.getProfile(userId, new SupabaseApi.ProfileCallback() {
            @Override
            public void onSuccess(Profile profile) {
                cache.saveProfile(profile);
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    public boolean isLoggedIn() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        return token != null && !token.isEmpty();
    }

    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getCurrentUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    private void saveSession(String accessToken, String userId, String email) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .apply();

        SupabaseApi.setStoredToken(accessToken);
    }

    private void clearSession() {
        String userId = prefs.getString(KEY_USER_ID, null);

        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .apply();

        SupabaseApi.setStoredToken(null);

        if (userId != null) {
            cache.clearProfile(userId);
        }
        cache.clearAll();
    }

    public void clearSessionForced() {
        clearSession();
    }

    private String parseAuthError(String message) {
        if (message == null) return "Неизвестная ошибка";

        if (message.contains("User already registered")) {
            return "Пользователь с таким email уже зарегистрирован";
        }
        if (message.contains("Invalid login credentials")) {
            return "Неверный email или пароль";
        }
        if (message.contains("Email not confirmed")) {
            return "Подтвердите email перед входом";
        }
        if (message.contains("Password should be at least")) {
            return "Пароль слишком короткий";
        }
        if (message.contains("Over request rate limit")) {
            return "Слишком много попыток, попробуйте позже";
        }

        if (message.contains("email_address_invalid")) {
            return "Некорректный email. Попробуйте другой адрес";
        }
        if (message.contains("email_provider_blocked")) {
            return "Этот почтовый провайдер временно недоступен";
        }
        if (message.contains("over_email_send_rate_limit")) {
            return "Слишком много запросов. Попробуйте позже";
        }

        return message;
    }

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface ProfileCallback {
        void onSuccess(Profile profile);

        void onError(String message);
    }
}