package com.parentcontrolapp.agent.ui.tabs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.ActivityLog;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;
import com.parentcontrolapp.agent.ui.adapters.ScreenshotAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TabScreenshotsFragment extends Fragment {

    private String deviceId;
    private DeviceRepository deviceRepo;

    private RecyclerView recyclerScreenshots;
    private ProgressBar progressLoading;
    private View layoutEmpty;
    private TextView tvCount;

    private List<ActivityLog> allScreenshots = new ArrayList<>();
    private ScreenshotAdapter adapter;

    private static final int PAGE_SIZE = 30;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentPage = 0;

    private final SimpleDateFormat moscowTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_screenshots, container, false);
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

        moscowTimeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

        initViews(view);
        loadScreenshots();
    }

    private void initViews(View view) {
        recyclerScreenshots = view.findViewById(R.id.recycler_screenshots);
        progressLoading = view.findViewById(R.id.progress_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvCount = view.findViewById(R.id.tv_screenshots_count);

        recyclerScreenshots.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerScreenshots.setHasFixedSize(true);

        adapter = new ScreenshotAdapter(new ArrayList<>(), this::showScreenshotFullscreen);
        recyclerScreenshots.setAdapter(adapter);

        recyclerScreenshots.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) {
                    GridLayoutManager lm = (GridLayoutManager) rv.getLayoutManager();
                    if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 4) {
                        if (hasMore && !isLoading) loadScreenshots();
                    }
                }
            }
        });
    }

    private void loadScreenshots() {
        if (isLoading || !hasMore) return;
        isLoading = true;
        showLoading(true);

        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        String endDate = sdf.format(new Date());
        String startDate = calculateStartDate(30);

        deviceRepo.fetchActivityLogs(deviceId, startDate, endDate, "screenshot", null, PAGE_SIZE, currentPage * PAGE_SIZE,
                new DeviceRepository.ActivityListCallback() {
                    @Override
                    public void onSuccess(List<ActivityLog> logs) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isLoading = false;
                            showLoading(false);

                            if (logs == null || logs.isEmpty()) {
                                hasMore = false;
                            } else {
                                int addedCount = 0;
                                for (ActivityLog log : logs) {
                                    if (log.screenshotUrl != null && !log.screenshotUrl.trim().isEmpty()) {
                                        allScreenshots.add(log);
                                        addedCount++;
                                    }
                                }

                                currentPage++;
                                hasMore = logs.size() == PAGE_SIZE;

                                if (addedCount == 0 && hasMore && allScreenshots.isEmpty()) {
                                    loadScreenshots();
                                    return;
                                }
                            }
                            updateUI();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isLoading = false;
                            showLoading(false);
                            Toast.makeText(getContext(), "Не удалось загрузить: " + message, Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }
                });
    }

    private void updateUI() {
        adapter.updateData(allScreenshots);

        if (allScreenshots.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerScreenshots.setVisibility(View.GONE);
            tvCount.setText("Нет скриншотов");
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerScreenshots.setVisibility(View.VISIBLE);
            tvCount.setText("Всего: " + allScreenshots.size() + (hasMore ? "+" : ""));
        }
    }

    private void showLoading(boolean show) {
        if (progressLoading != null) {
            progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showScreenshotFullscreen(ActivityLog screenshot) {
        if (screenshot.screenshotUrl == null) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_screenshot_fullscreen);

        ImageView imgFullscreen = dialog.findViewById(R.id.img_fullscreen);
        TextView tvFullscreenTime = dialog.findViewById(R.id.tv_fullscreen_time);
        View btnClose = dialog.findViewById(R.id.btn_close);

        Glide.with(requireContext())
                .load(screenshot.screenshotUrl)
                .placeholder(R.drawable.bg_chart_placeholder)
                .error(R.drawable.bg_chart_placeholder)
                .into(imgFullscreen);

        String timeString;
        if (screenshot.occurredAt != null) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                String cleanDate = screenshot.occurredAt.replace("Z", "").split("\\+")[0];
                Date date = isoFormat.parse(cleanDate);

                if (date != null) {
                    timeString = moscowTimeFormat.format(date);
                } else {
                    timeString = screenshot.occurredAt;
                }
            } catch (Exception e) {
                timeString = screenshot.occurredAt;
            }
        } else {
            timeString = "Неизвестно";
        }

        tvFullscreenTime.setText(timeString);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String calculateStartDate(int daysBack) {
        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        java.util.Calendar cal = java.util.Calendar.getInstance(moscowTimeZone);
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysBack);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        return sdf.format(cal.getTime());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerScreenshots = null;
        progressLoading = null;
        layoutEmpty = null;
        adapter = null;
    }
}