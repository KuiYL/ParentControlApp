package com.parentcontrolapp.agent.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parentcontrolapp.agent.R;

import java.io.File;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private static final String PREF_NAME = "app_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_PUSH_ENABLED = "push_enabled";

    private SharedPreferences prefs;

    private TextView tvThemeValue, tvLanguageValue, tvCacheSize;
    private SwitchCompat switchPush;
    private View btnTheme, btnLanguage, btnClearCache;

    private boolean isUpdatingPush = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        initViews(view);

        loadSettings();

        setupClickListeners();
    }

    private void initViews(View view) {
        tvThemeValue = view.findViewById(R.id.tv_theme_value);
        tvLanguageValue = view.findViewById(R.id.tv_language_value);
        btnTheme = view.findViewById(R.id.btn_theme);
        btnLanguage = view.findViewById(R.id.btn_language);

        switchPush = view.findViewById(R.id.switch_push);

        tvCacheSize = view.findViewById(R.id.tv_cache_size);
        btnClearCache = view.findViewById(R.id.btn_clear_cache);
    }

    /**
     * Загружает сохранённые настройки в интерфейс
     */
    private void loadSettings() {
        String theme = prefs.getString(KEY_THEME, "system");
        tvThemeValue.setText(getThemeDisplayName(theme));

        String language = prefs.getString(KEY_LANGUAGE, "ru");
        tvLanguageValue.setText(getLanguageDisplayName(language));

        boolean pushEnabled = prefs.getBoolean(KEY_PUSH_ENABLED, true);

        isUpdatingPush = true;
        switchPush.setChecked(pushEnabled);
        isUpdatingPush = false;

        updateCacheSize();
    }

    /**
     * Настраивает обработчики кликов
     */
    private void setupClickListeners() {
        btnTheme.setOnClickListener(v -> showThemeDialog());

        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingPush) return;

            prefs.edit().putBoolean(KEY_PUSH_ENABLED, isChecked).apply();
            Toast.makeText(getContext(), isChecked ? "Уведомления включены" : "Уведомления отключены", Toast.LENGTH_SHORT).show();
        });

        btnClearCache.setOnClickListener(v -> confirmClearCache());
    }

    private void showThemeDialog() {
        String[] themes = {"Системная", "Светлая", "Тёмная"};
        String[] themeValues = {"system", "light", "dark"};

        String currentTheme = prefs.getString(KEY_THEME, "system");
        int checkedIndex = 0;
        for (int i = 0; i < themeValues.length; i++) {
            if (themeValues[i].equals(currentTheme)) {
                checkedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_AppTheme_AlertDialog)
                .setTitle("Тема оформления")
                .setSingleChoiceItems(themes, checkedIndex, (dialog, which) -> {
                    String selectedValue = themeValues[which];

                    prefs.edit().putString(KEY_THEME, selectedValue).apply();
                    tvThemeValue.setText(themes[which]);

                    applyTheme(selectedValue);

                    dialog.dismiss();
                    Toast.makeText(getContext(), "Тема применена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyTheme(String themeMode) {
        int mode;
        switch (themeMode) {
            case "light":
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(mode);
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private String getThemeDisplayName(String themeValue) {
        switch (themeValue) {
            case "light":
                return "Светлая";
            case "dark":
                return "Тёмная";
            default:
                return "Системная";
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"Русский"};
        String[] langCodes = {"ru"};

        String currentLang = prefs.getString(KEY_LANGUAGE, "ru");
        int checkedIndex = 0;
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentLang)) {
                checkedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_AppTheme_AlertDialog)
                .setTitle("Язык приложения")
                .setSingleChoiceItems(languages, checkedIndex, (dialog, which) -> {
                    String selectedLang = langCodes[which];
                    prefs.edit().putString(KEY_LANGUAGE, selectedLang).apply();
                    tvLanguageValue.setText(languages[which]);
                    applyLanguage(selectedLang);
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Язык изменён. Перезапустите приложение", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void applyLanguage(String langCode) {
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        requireContext().getResources().updateConfiguration(config, requireContext().getResources().getDisplayMetrics());

        requireActivity().recreate();
    }

    private String getLanguageDisplayName(String langCode) {
        return "ru".equals(langCode) ? "Русский" : "English";
    }

    private void updateCacheSize() {
        long cacheSize = getCacheSize();
        tvCacheSize.setText(formatFileSize(cacheSize));
    }

    private long getCacheSize() {
        long size = 0;
        try {
            File cacheDir = requireContext().getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                size = getFolderSize(cacheDir);
            }

            File glideCache = new File(requireContext().getCacheDir(), "image_manager_disk_cache");
            if (glideCache.exists()) {
                size += getFolderSize(glideCache);
            }
        } catch (Exception ignored) {
        }
        return size;
    }

    private long getFolderSize(File folder) {
        long size = 0;
        if (folder.listFiles() != null) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getFolderSize(file);
                }
            }
        }
        return size;
    }

    @SuppressLint("DefaultLocale")
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return String.format("%.1f КБ", bytes / 1024f);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f МБ", bytes / (1024f * 1024f));
        return String.format("%.1f ГБ", bytes / (1024f * 1024f * 1024f));
    }

    private void confirmClearCache() {
        long cacheSize = getCacheSize();
        if (cacheSize == 0) {
            Toast.makeText(getContext(), "Кэш уже пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_AppTheme_AlertDialog)
                .setTitle("Очистить кэш?")
                .setMessage("Будет удалено ~" + formatFileSize(cacheSize) + "\n\nЭто не затронет ваши данные и настройки.")
                .setPositiveButton("Очистить", (dialog, which) -> clearCache())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void clearCache() {
        try {
            File cacheDir = requireContext().getCacheDir();
            int deletedCount = 0;

            if (cacheDir != null && cacheDir.exists()) {
                deletedCount = deleteFolderContents(cacheDir);
            }

            updateCacheSize();

            Toast.makeText(getContext(), "Кэш очищен", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Не удалось очистить кэш", Toast.LENGTH_SHORT).show();
        }
    }

    private int deleteFolderContents(File folder) {
        int count = 0;
        if (folder.listFiles() != null) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    if (file.delete()) {
                        count++;
                    }
                } else if (file.isDirectory()) {
                    count += deleteFolderContents(file);
                    file.delete();
                }
            }
        }
        return count;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCacheSize();
    }
}