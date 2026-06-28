package com.parentcontrolapp.agent.ui.tabs;

import static com.parentcontrolapp.agent.utils.TimeUtils.formatMinutes;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.ActivityLog;
import com.parentcontrolapp.agent.ui.adapters.AppUsageItem;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;
import com.parentcontrolapp.agent.ui.adapters.SimpleAppAdapter;
import com.parentcontrolapp.agent.ui.custom.BarChartView;
import com.parentcontrolapp.agent.ui.custom.PieChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TabActivityFragment extends Fragment {

    private String deviceId;
    private DeviceRepository deviceRepo;

    private LinearLayout layoutLoading, layoutContent;
    private View layoutEmpty;

    private TextView tvTotalTime, tvLaunches, tvBlocks;
    private RecyclerView recyclerTopApps;

    private SimpleAppAdapter appAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PieChartView pieChart;
    private LinearLayout layoutLegend;

    private static final int[] PIE_COLORS = {Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"), Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"), Color.parseColor("#F44336"), Color.parseColor("#00BCD4"), Color.parseColor("#FFEB3B"), Color.parseColor("#795548"), Color.parseColor("#607D8B"), Color.parseColor("#E91E63")};

    private BarChartView barChart;

    private static final int[] BAR_COLORS = {Color.parseColor("#BBDEFB"), Color.parseColor("#90CAF9"), Color.parseColor("#64B5F6"), Color.parseColor("#42A5F5"), Color.parseColor("#2196F3"), Color.parseColor("#1E88E5"), Color.parseColor("#1565C0")};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_activity, container, false);
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
        loadData();
    }

    private void initViews(View view) {
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutContent = view.findViewById(R.id.layout_content);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        View btnRetry = view.findViewById(R.id.btn_retry);

        tvTotalTime = view.findViewById(R.id.tv_total_time);
        tvLaunches = view.findViewById(R.id.tv_launches);
        tvBlocks = view.findViewById(R.id.tv_blocks);
        recyclerTopApps = view.findViewById(R.id.recycler_top_apps);

        pieChart = view.findViewById(R.id.pie_chart);
        layoutLegend = view.findViewById(R.id.layout_legend);
        barChart = view.findViewById(R.id.bar_chart);

        recyclerTopApps.setLayoutManager(new LinearLayoutManager(getContext()));

        appAdapter = new SimpleAppAdapter();
        recyclerTopApps.setAdapter(appAdapter);

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadData());
        }
    }

    private void showState(State state) {
        if (layoutLoading != null) {
            layoutLoading.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        }
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        }
        if (layoutContent != null) {
            layoutContent.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        }
    }

    private void loadData() {
        showState(State.LOADING);
        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        String endDate = sdf.format(new Date());
        String startDate = calculateStartDate();

        deviceRepo.fetchActivityLogs(deviceId, startDate, endDate, "all", null, 500, 0, new DeviceRepository.ActivityListCallback() {
            @Override
            public void onSuccess(List<ActivityLog> logs) {
                if (!isAdded()) return;

                executor.execute(() -> {
                    ProcessedData data = processLogs(logs);

                    requireActivity().runOnUiThread(() -> {
                        if (logs != null && !logs.isEmpty()) {
                            showState(State.CONTENT);
                            updateUI(data);
                        } else {
                            showState(State.EMPTY);
                        }
                    });
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Не удалось загрузить: " + message, Toast.LENGTH_SHORT).show();
                    showState(State.EMPTY);
                });
            }
        });
    }

    private void updateUI(ProcessedData data) {
        if (!isAdded() || getContext() == null) return;

        tvTotalTime.setText(formatMinutes(data.totalMinutes));
        tvLaunches.setText(String.valueOf(data.launches));
        tvBlocks.setText(String.valueOf(data.blocks));

        updatePieChart(data.appItems);
        updateBarChart(data.dailyMinutes);

        appAdapter.submitList(data.appItems);
    }

    private void updatePieChart(List<AppUsageItem> appItems) {
        if (appItems == null || appItems.isEmpty()) {
            pieChart.setData(new ArrayList<>());
            layoutLegend.removeAllViews();
            return;
        }

        int topAppsTotalMinutes = 0;
        for (AppUsageItem item : appItems) {
            topAppsTotalMinutes += item.timeMinutes;
        }

        if (topAppsTotalMinutes == 0) {
            pieChart.setData(new ArrayList<>());
            layoutLegend.removeAllViews();
            return;
        }

        List<PieChartView.PieSlice> slices = new ArrayList<>();
        layoutLegend.removeAllViews();

        for (int i = 0; i < appItems.size(); i++) {
            AppUsageItem item = appItems.get(i);
            float percentage = (float) item.timeMinutes / topAppsTotalMinutes;

            int color = PIE_COLORS[i % PIE_COLORS.length];
            slices.add(new PieChartView.PieSlice(item.appName, percentage, color));

            View legendItem = LayoutInflater.from(getContext()).inflate(R.layout.item_pie_legend, layoutLegend, false);
            View viewColor = legendItem.findViewById(R.id.view_color);
            TextView tvLabel = legendItem.findViewById(R.id.tv_label);
            TextView tvPercentage = legendItem.findViewById(R.id.tv_percentage);

            viewColor.setBackgroundColor(color);
            tvLabel.setText(item.appName);
            tvPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", percentage * 100));

            layoutLegend.addView(legendItem);
        }

        pieChart.setData(slices);
    }

    private void updateBarChart(Map<String, Integer> dailyMinutes) {
        if (dailyMinutes == null) {
            barChart.setData(new ArrayList<>());
            return;
        }

        List<BarChartView.BarData> barData = new ArrayList<>();
        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        Calendar cal = Calendar.getInstance(moscowTimeZone);

        String[] dayNames = {"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateKeyFormat.setTimeZone(moscowTimeZone);

        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);

            String dateKey = dateKeyFormat.format(dayCal.getTime());

            int dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK);
            String dayLabel = dayNames[dayOfWeek - 1];

            int minutes = dailyMinutes.getOrDefault(dateKey, 0);
            int color;
            if (minutes == 0) {
                color = Color.parseColor("#E0E0E0");
            } else if (i == 0) {
                color = Color.parseColor("#F44336");
            } else if (i == 1) {
                color = Color.parseColor("#FF5722");
            } else {
                color = BAR_COLORS[i % BAR_COLORS.length];
            }

            barData.add(new BarChartView.BarData(dayLabel, minutes, color));
        }

        barChart.setData(barData);
    }

    private String calculateStartDate() {
        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        Calendar cal = Calendar.getInstance(moscowTimeZone);
        cal.add(Calendar.DAY_OF_YEAR, -30);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        return sdf.format(cal.getTime());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        layoutLoading = layoutContent = null;
        tvTotalTime = tvLaunches = tvBlocks = null;
        recyclerTopApps = null;
        appAdapter = null;
        pieChart = null;
        layoutLegend = null;
        barChart = null;
    }

    private static class ProcessedData {
        int totalMinutes;
        int launches;
        int blocks;
        List<AppUsageItem> appItems;
        Map<String, Integer> dailyMinutes;

        ProcessedData(int totalMinutes, int launches, int blocks, List<AppUsageItem> appItems, Map<String, Integer> dailyMinutes) {
            this.totalMinutes = totalMinutes;
            this.launches = launches;
            this.blocks = blocks;
            this.appItems = appItems;
            this.dailyMinutes = dailyMinutes;
        }
    }

    private enum State {
        LOADING, EMPTY, CONTENT
    }

    private ProcessedData processLogs(List<ActivityLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return new ProcessedData(0, 0, 0, new ArrayList<>(), new HashMap<>());
        }

        long totalSeconds = 0;
        int launches = 0;
        int blocks = 0;
        Map<String, Long> appTimeSeconds = new HashMap<>();
        Map<String, Integer> dailyMinutes = new HashMap<>();

        SimpleDateFormat isoFormatWithMs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        isoFormatWithMs.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat isoFormatNoMs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        isoFormatNoMs.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat moscowDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        moscowDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

        for (ActivityLog log : logs) {
            String eventType = log.eventType != null ? log.eventType : "";

            if ("app_launch".equals(eventType) || "web_visit".equals(eventType) || "window_change".equals(eventType)) {
                launches++;

                if (log.durationSeconds > 0) {
                    totalSeconds += log.durationSeconds;

                    if (log.appName != null && !log.appName.isEmpty()) {
                        appTimeSeconds.put(log.appName, appTimeSeconds.getOrDefault(log.appName, 0L) + log.durationSeconds);
                    }

                    if (log.occurredAt != null && !log.occurredAt.isEmpty()) {
                        try {
                            String cleanDate = log.occurredAt;

                            if (cleanDate.contains("T")) {
                                cleanDate = cleanDate.replace("T", " ");
                            }

                            if (cleanDate.contains("+")) {
                                cleanDate = cleanDate.substring(0, cleanDate.indexOf("+"));
                            } else if (cleanDate.endsWith("Z")) {
                                cleanDate = cleanDate.substring(0, cleanDate.length() - 1);
                            }

                            Date dateUtc = null;

                            try {
                                if (cleanDate.contains(".")) {
                                    String[] parts = cleanDate.split("\\.");
                                    String msPart = parts[1];
                                    if (msPart.length() > 3) msPart = msPart.substring(0, 3);
                                    cleanDate = parts[0] + "." + msPart;
                                }
                                dateUtc = isoFormatWithMs.parse(cleanDate);
                            } catch (Exception e) {
                                dateUtc = isoFormatNoMs.parse(cleanDate);
                            }

                            if (dateUtc != null) {
                                String moscowDateKey = moscowDateFormat.format(dateUtc);

                                int minutes = log.durationSeconds / 60;
                                if (minutes < 1) minutes = 1;

                                dailyMinutes.put(moscowDateKey, dailyMinutes.getOrDefault(moscowDateKey, 0) + minutes);
                            }
                        } catch (Exception e) {
                            try {
                                if (log.occurredAt.length() >= 10) {
                                    String logDate = log.occurredAt.substring(0, 10);
                                    int minutes = log.durationSeconds / 60;
                                    if (minutes < 1) minutes = 1;
                                    dailyMinutes.put(logDate, dailyMinutes.getOrDefault(logDate, 0) + minutes);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            if ("block_triggered".equals(eventType) || "limit_reached".equals(eventType)) {
                blocks++;
            }
        }

        int totalMinutes = (int) (totalSeconds / 60);

        List<AppUsageItem> appItems = new ArrayList<>();
        for (Map.Entry<String, Long> entry : appTimeSeconds.entrySet()) {
            long seconds = entry.getValue();
            if (seconds > 0) {
                int minutes = (int) Math.max(1, seconds / 60);
                appItems.add(new AppUsageItem(entry.getKey(), minutes));
            }
        }

        appItems.sort((a, b) -> b.timeMinutes - a.timeMinutes);
        if (appItems.size() > 7) {
            appItems = new ArrayList<>(appItems.subList(0, 7));
        }

        return new ProcessedData(totalMinutes, launches, blocks, appItems, dailyMinutes);
    }
}