package com.parentcontrolapp.agent.ui.tabs;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.DeviceRules;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;

import java.util.Calendar;
import java.util.Locale;

public class TabDeviceSettingsFragment extends Fragment {

    private String deviceId;
    private DeviceRepository deviceRepo;

    private LinearLayout layoutLoading, layoutContent;
    private TextView tvUsedTime, tvLimit, tvRemaining;
    private ProgressBar progressLimitHeader;

    private SwitchCompat switchLimitEnabled;
    private Slider sliderLimit;
    private TextView tvLimitValue, tvUsedValue, tvProgressLabel, tvLimitDisabled;
    private ProgressBar progressLimitSetting;
    private LinearLayout containerLimitSlider;

    private SwitchCompat switchNightMode;
    private LinearLayout layoutNightTime;
    private TextView tvNightStart, tvNightEnd;

    private SwitchCompat switchAdultBlock, switchSocialBlock, switchGamesBlock;

    private MaterialButton btnSave, btnReset;

    private DeviceRules currentRules;
    private int currentUsedMinutes = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_device_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            deviceId = getArguments().getString("device_id");
        }
        if (deviceId == null) {
            Toast.makeText(getContext(), "Ошибка: не передан device_id", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceRepo = new DeviceRepository(requireContext());
        initViews(view);
        loadSettings();
        setupListeners();
    }

    private void initViews(View view) {
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutContent = view.findViewById(R.id.layout_content);

        tvUsedTime = view.findViewById(R.id.tv_used_time);
        tvLimit = view.findViewById(R.id.tv_limit);
        tvRemaining = view.findViewById(R.id.tv_remaining);
        progressLimitHeader = view.findViewById(R.id.progress_limit);

        switchLimitEnabled = view.findViewById(R.id.switch_limit_enabled);
        sliderLimit = view.findViewById(R.id.slider_limit);
        tvLimitValue = view.findViewById(R.id.tv_limit_value);
        tvUsedValue = view.findViewById(R.id.tv_used_value);
        tvProgressLabel = view.findViewById(R.id.tv_progress_label);
        tvLimitDisabled = view.findViewById(R.id.tv_limit_disabled);
        progressLimitSetting = view.findViewById(R.id.progress_limit_setting);
        containerLimitSlider = view.findViewById(R.id.container_limit_slider);

        switchNightMode = view.findViewById(R.id.switch_night_mode);
        layoutNightTime = view.findViewById(R.id.layout_night_time);
        tvNightStart = view.findViewById(R.id.tv_night_start);
        tvNightEnd = view.findViewById(R.id.tv_night_end);

        switchAdultBlock = view.findViewById(R.id.switch_adult_block);
        switchSocialBlock = view.findViewById(R.id.switch_social_block);
        switchGamesBlock = view.findViewById(R.id.switch_games_block);

        btnSave = view.findViewById(R.id.btn_save_settings);
        btnReset = view.findViewById(R.id.btn_reset);
    }

    private void setLoading(boolean loading) {
        if (layoutLoading != null && layoutContent != null) {
            layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            layoutContent.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private void loadSettings() {
        setLoading(true);
        deviceRepo.fetchDeviceRules(deviceId, new DeviceRepository.RulesCallback() {
            @Override
            public void onSuccess(DeviceRules rules) {
                requireActivity().runOnUiThread(() -> {
                    currentRules = rules;
                    fetchAndBindUsedTime(rules);
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Не удалось загрузить настройки", Toast.LENGTH_SHORT).show();
                    DeviceRules defaults = createDefaultRules();
                    currentRules = defaults;
                    fetchAndBindUsedTime(defaults);
                });
            }
        });
    }

    /**
     * Загружает использованное время и обновляет UI
     */
    private void fetchAndBindUsedTime(DeviceRules rules) {
        deviceRepo.fetchUsedTimeToday(deviceId, new DeviceRepository.UsedTimeCallback() {
            @Override
            public void onSuccess(int minutes) {
                requireActivity().runOnUiThread(() -> {
                    currentUsedMinutes = minutes;
                    bindSettings(rules);
                    setLoading(false);
                });
            }

            @Override
            public void onError(String message) {
                currentUsedMinutes = 0;
                bindSettings(rules);
                setLoading(false);
            }
        });
    }

    private void bindSettings(DeviceRules rules) {
        if (rules == null) {
            rules = createDefaultRules();
        }

        int limitMinutes = rules.getDailyLimitMinutes() != null ? rules.getDailyLimitMinutes() : 120;
        if (rules.getDailyLimitMinutes() == null) {
            rules.dailyLimitMinutes = limitMinutes;
        }
        int remaining = Math.max(0, limitMinutes - currentUsedMinutes);

        tvUsedTime.setText(formatMinutes(currentUsedMinutes));
        tvLimit.setText(formatMinutes(limitMinutes));
        tvRemaining.setText(formatMinutes(remaining));

        if (remaining > 0) {
            tvRemaining.setTextColor(requireContext().getColor(R.color.green_accent));
        } else {
            tvRemaining.setTextColor(requireContext().getColor(R.color.error));
        }

        tvUsedValue.setText(formatMinutes(currentUsedMinutes));
        tvLimitValue.setText(formatMinutes(limitMinutes));

        boolean limitEnabled = rules.limitEnabled != null && rules.limitEnabled;
        switchLimitEnabled.setChecked(limitEnabled);

        sliderLimit.setEnabled(limitEnabled);
        containerLimitSlider.setAlpha(limitEnabled ? 1.0f : 0.5f);
        tvLimitDisabled.setVisibility(limitEnabled ? View.GONE : View.VISIBLE);

        if (limitEnabled) {
            int safeLimit = Math.min(Math.max(limitMinutes, 30), 480);
            sliderLimit.setValue(safeLimit);
        }

        boolean nightEnabled = rules.nightModeEnabled != null && rules.nightModeEnabled;
        switchNightMode.setChecked(nightEnabled);
        layoutNightTime.setVisibility(nightEnabled ? View.VISIBLE : View.GONE);

        tvNightStart.setText(parseTime(rules.nightModeStart, "23:00"));
        tvNightEnd.setText(parseTime(rules.nightModeEnd, "07:00"));

        switchAdultBlock.setChecked(rules.blockAdultContent != null && rules.blockAdultContent);
        switchSocialBlock.setChecked(rules.blockSocialNetworks != null && rules.blockSocialNetworks);
        switchGamesBlock.setChecked(rules.blockGames != null && rules.blockGames);

        updateProgressUI(currentUsedMinutes, limitMinutes, limitEnabled);
    }

    @SuppressLint("SetTextI18n")
    private void updateProgressUI(int used, int limit, boolean limitEnabled) {
        int remaining = Math.max(0, limit - used);
        int progress = limitEnabled ? Math.min(100, (used * 100) / Math.max(1, limit)) : 0;

        progressLimitHeader.setProgress(progress);
        progressLimitSetting.setProgress(progress);

        if (!limitEnabled) {
            tvProgressLabel.setText("Без лимита");
            tvProgressLabel.setTextColor(requireContext().getColor(R.color.text_hint));
        } else {
            int diff = Math.abs(remaining);
            tvProgressLabel.setText((remaining > 0 ? "−" : "+") + diff + " мин");
            tvProgressLabel.setTextColor(remaining >= 0 ? requireContext().getColor(R.color.green_700) : requireContext().getColor(R.color.error));
        }
    }

    private void setupListeners() {
        switchLimitEnabled.setOnCheckedChangeListener((button, isChecked) -> {
            sliderLimit.setEnabled(isChecked);
            containerLimitSlider.setAlpha(isChecked ? 1.0f : 0.5f);
            tvLimitDisabled.setVisibility(isChecked ? View.GONE : View.VISIBLE);

            if (currentRules != null) {
                currentRules.limitEnabled = isChecked;
                int limit = currentRules.dailyLimitMinutes != null ? currentRules.dailyLimitMinutes : 120;
                updateProgressUI(currentUsedMinutes, limit, isChecked);
            }
        });

        sliderLimit.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && currentRules != null) {
                int newLimit = (int) value;
                currentRules.dailyLimitMinutes = newLimit;

                tvLimitValue.setText(formatMinutes(newLimit));
                tvLimit.setText(formatMinutes(newLimit));

                boolean limitEnabled = currentRules.limitEnabled != null && currentRules.limitEnabled;
                updateProgressUI(currentUsedMinutes, newLimit, limitEnabled);

                int remaining = Math.max(0, newLimit - currentUsedMinutes);
                tvRemaining.setText(formatMinutes(remaining));
                if (remaining > 0) {
                    tvRemaining.setTextColor(requireContext().getColor(R.color.green_accent));
                } else {
                    tvRemaining.setTextColor(requireContext().getColor(R.color.error));
                }
            }
        });

        switchNightMode.setOnCheckedChangeListener((button, isChecked) -> {
            layoutNightTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (currentRules != null) {
                currentRules.nightModeEnabled = isChecked;
            }
        });

        tvNightStart.setOnClickListener(v -> showTimePicker(tvNightStart, start -> {
            if (currentRules != null) {
                currentRules.nightModeStart = start + ":00";
            }
        }));

        tvNightEnd.setOnClickListener(v -> showTimePicker(tvNightEnd, end -> {
            if (currentRules != null) {
                currentRules.nightModeEnd = end + ":00";
            }
        }));

        switchAdultBlock.setOnCheckedChangeListener((b, isChecked) -> {
            if (currentRules != null) currentRules.blockAdultContent = isChecked;
        });
        switchSocialBlock.setOnCheckedChangeListener((b, isChecked) -> {
            if (currentRules != null) currentRules.blockSocialNetworks = isChecked;
        });
        switchGamesBlock.setOnCheckedChangeListener((b, isChecked) -> {
            if (currentRules != null) currentRules.blockGames = isChecked;
        });

        btnReset.setOnClickListener(v -> resetToDefaults());
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void showTimePicker(TextView target, TimeCallback callback) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        String currentTime = target.getText().toString();
        try {
            String[] parts = currentTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
        }

        new TimePickerDialog(getContext(), (view, selectedHour, selectedMinute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            target.setText(time);
            callback.onTimeSelected(time);
        }, hour, minute, true).show();
    }

    interface TimeCallback {
        void onTimeSelected(String time);
    }

    private void resetToDefaults() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Сбросить настройки?")
                .setMessage("Все настройки вернутся к значениям по умолчанию")
                .setPositiveButton("Сбросить", (d, w) -> {
                    DeviceRules defaults = createDefaultRules();
                    bindSettings(defaults);
                    currentRules = defaults;
                    Toast.makeText(getContext(), "Настройки сброшены", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveSettings() {
        if (currentRules == null) {
            Toast.makeText(getContext(), "Ошибка: настройки не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SettingsDebug", "Saving rules: " +
                "limit=" + currentRules.dailyLimitMinutes +
                ", enabled=" + currentRules.limitEnabled +
                ", night=" + currentRules.nightModeEnabled +
                ", adult=" + currentRules.blockAdultContent);

        btnSave.setEnabled(false);
        btnSave.setText("Сохранение...");

        deviceRepo.updateDeviceRules(deviceId, currentRules, new DeviceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить изменения");
                    Toast.makeText(getContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить изменения");
                    Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private DeviceRules createDefaultRules() {
        DeviceRules rules = new DeviceRules();
        rules.dailyLimitMinutes = 120;
        rules.limitEnabled = true;
        rules.nightModeEnabled = false;
        rules.nightModeStart = "23:00:00";
        rules.nightModeEnd = "07:00:00";
        rules.blockAdultContent = true;
        rules.blockSocialNetworks = false;
        rules.blockGames = false;
        return rules;
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " мин";
        int h = minutes / 60, m = minutes % 60;
        return h + "ч " + (m > 0 ? m + "мин" : "");
    }

    private String parseTime(String timeStr, String defaultValue) {
        if (timeStr == null || timeStr.isEmpty()) return defaultValue;
        try {
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                if (parts.length >= 2) {
                    return parts[0] + ":" + parts[1];
                }
            }
            return timeStr;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvUsedTime = tvLimit = tvRemaining = null;
        progressLimitHeader = null;
        switchLimitEnabled = null;
        sliderLimit = null;
        tvLimitValue = tvUsedValue = tvProgressLabel = tvLimitDisabled = null;
        progressLimitSetting = null;
        containerLimitSlider = null;
        switchNightMode = null;
        layoutNightTime = null;
        tvNightStart = tvNightEnd = null;
        switchAdultBlock = switchSocialBlock = switchGamesBlock = null;
        btnSave = btnReset = null;
        currentRules = null;
    }
}