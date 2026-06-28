package com.parentcontrolapp.agent.ui.tabs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.ActivityLog;
import com.parentcontrolapp.agent.data.model.DeviceRules;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;
import com.parentcontrolapp.agent.ui.adapters.TimelineGroupedAdapter;
import com.parentcontrolapp.agent.ui.adapters.TimelineItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TabTimelineFragment extends Fragment {

    private static final int PAGE_SIZE = 20;
    private static final long SEARCH_DEBOUNCE_MS = 300;

    private String deviceId;
    private DeviceRepository deviceRepo;

    private RecyclerView recyclerView;
    private ChipGroup chipGroup;
    private Button btnLoadMore;
    private TextInputEditText editSearch;

    private LinearLayout layoutLoading, layoutEmpty, layoutContent;
    private LinearLayout layoutEmptyFilter;

    private TimelineGroupedAdapter adapter;

    private final List<ActivityLog> allLogs = new ArrayList<>();
    private final Object logsLock = new Object();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isFirstLoad = true;

    private String currentFilter = "all";
    private String searchQuery = "";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SwipeRefreshLayout swipeRefresh;

    private final SimpleDateFormat moscowDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            deviceId = getArguments().getString("device_id");
        }

        if (deviceId == null) {
            Toast.makeText(getContext(), "Ошибка: не передан deviceId", Toast.LENGTH_SHORT).show();
            return;
        }

        deviceRepo = new DeviceRepository(requireContext());

        // Инициализируем форматтер московским временем
        moscowDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));

        initViews(view);
        setupFilters();
        setupSearch();
        loadLogs();
    }

    private void initViews(View view) {
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutContent = view.findViewById(R.id.layout_content);
        layoutEmptyFilter = view.findViewById(R.id.layout_empty_filter);

        recyclerView = view.findViewById(R.id.recycler_timeline);
        chipGroup = view.findViewById(R.id.chip_filters);
        btnLoadMore = view.findViewById(R.id.btn_load_more);
        editSearch = view.findViewById(R.id.edit_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.green_600);
            swipeRefresh.setOnRefreshListener(this::resetAndReload);
        }

        adapter = new TimelineGroupedAdapter(this::showBlockConfirmationDialog);
        recyclerView.setAdapter(adapter);

        btnLoadMore.setOnClickListener(v -> loadMore());
    }

    private void setupSearch() {
        if (editSearch == null) return;
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    String newQuery = s.toString().toLowerCase(Locale.getDefault()).trim();
                    if (!newQuery.equals(searchQuery)) {
                        searchQuery = newQuery;
                        resetAndReload();
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void setupFilters() {
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            String newFilter = "all";
            if (checkedId == R.id.filter_apps) newFilter = "apps";
            else if (checkedId == R.id.filter_web) newFilter = "web";
            else if (checkedId == R.id.filter_blocked) newFilter = "blocked";

            if (!newFilter.equals(currentFilter)) {
                currentFilter = newFilter;
                resetAndReload();
            }
        });
    }

    private void resetAndReload() {
        synchronized (logsLock) {
            currentPage = 0;
            allLogs.clear();
            hasMore = true;
            isFirstLoad = true;
        }

        if (adapter.getItemCount() == 0) {
            showGlobalState(GlobalState.LOADING);
        }
        loadLogs();
    }

    private void loadLogs() {
        if (isLoading || !hasMore) {
            if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }
        isLoading = true;
        updateLoadMoreButton();

        if (isFirstLoad) {
            showGlobalState(GlobalState.LOADING);
        }

        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        String endDate = sdf.format(new Date());
        String startDate = calculateStartDate();

        deviceRepo.fetchActivityLogs(deviceId, startDate, endDate, currentFilter, searchQuery,
                PAGE_SIZE, currentPage * PAGE_SIZE,
                new DeviceRepository.ActivityListCallback() {
                    @Override
                    public void onSuccess(List<ActivityLog> logs) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            isLoading = false;
                            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                            synchronized (logsLock) {
                                if (logs == null || logs.isEmpty()) {
                                    hasMore = false;
                                } else {
                                    allLogs.addAll(logs);
                                    hasMore = logs.size() == PAGE_SIZE;
                                    currentPage++;
                                }
                            }

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                synchronized (logsLock) {
                                    if (allLogs.isEmpty()) {
                                        showGlobalState(GlobalState.EMPTY);
                                    } else {
                                        showGlobalState(GlobalState.CONTENT);
                                        updateAdapter();
                                    }
                                }
                            } else {
                                updateAdapter();
                            }
                            updateLoadMoreButton();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Не удалось загрузить: " + message, Toast.LENGTH_SHORT).show();
                            isLoading = false;
                            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                            if (isFirstLoad) {
                                isFirstLoad = false;
                                showGlobalState(GlobalState.EMPTY);
                            }
                            updateLoadMoreButton();
                        });
                    }
                });
    }

    private void updateAdapter() {
        showGlobalState(GlobalState.CONTENT);

        final List<ActivityLog> snapshot;
        synchronized (logsLock) {
            snapshot = new ArrayList<>(allLogs);
        }

        executor.execute(() -> {
            List<TimelineItem> grouped = TimelineItem.groupByDate(snapshot);

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                if (grouped.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    layoutEmptyFilter.setVisibility(View.VISIBLE);
                    btnLoadMore.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    layoutEmptyFilter.setVisibility(View.GONE);
                    adapter.updateData(grouped);
                }
            });
        });
    }

    private void loadMore() {
        if (hasMore && !isLoading) {
            loadLogs();
        }
    }

    private void updateLoadMoreButton() {
        if (btnLoadMore == null) return;

        if (isLoading) {
            btnLoadMore.setText("Загрузка...");
            btnLoadMore.setEnabled(false);
            btnLoadMore.setVisibility(View.VISIBLE);
        } else if (hasMore) {
            btnLoadMore.setText("Загрузить ещё");
            btnLoadMore.setEnabled(true);
            btnLoadMore.setVisibility(View.VISIBLE);
        } else {
            btnLoadMore.setVisibility(View.GONE);
        }
    }

    private void showBlockConfirmationDialog(ActivityLog log) {
        String target = null;
        boolean isApp = false;

        if ("app_launch".equals(log.eventType) || "app_close".equals(log.eventType)) {
            if (log.appName != null && !log.appName.isEmpty()) {
                target = log.appName;
                isApp = true;
            }
        } else if ("web_visit".equals(log.eventType)) {
            if (log.url != null && !log.url.isEmpty()) {
                try {
                    java.net.URI uri = java.net.URI.create(log.url);
                    target = uri.getHost();
                    if (target != null && target.startsWith("www.")) {
                        target = target.substring(4);
                    }
                } catch (Exception e) {
                    target = log.url;
                }
            }
        }

        if (target == null) {
            Toast.makeText(getContext(), "Не удалось определить, что блокировать", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalTarget = target;
        final boolean finalIsApp = isApp;
        String typeText = finalIsApp ? "приложение" : "сайт";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Заблокировать " + typeText + "?")
                .setMessage("Вы уверены, что хотите добавить \"" + finalTarget + "\" в черный список?")
                .setPositiveButton("Заблокировать", (dialog, which) -> {
                    performBlockItem(finalTarget, finalIsApp);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performBlockItem(String target, boolean isApp) {
        deviceRepo.fetchDeviceRules(deviceId, new DeviceRepository.RulesCallback() {
            @Override
            public void onSuccess(DeviceRules rules) {
                if (rules == null) rules = new DeviceRules();

                boolean alreadyBlocked = false;
                if (isApp && rules.blockedApps != null) {
                    for (String s : rules.blockedApps)
                        if (s.equalsIgnoreCase(target)) alreadyBlocked = true;
                } else if (!isApp && rules.blockedDomains != null) {
                    for (String s : rules.blockedDomains)
                        if (s.equalsIgnoreCase(target)) alreadyBlocked = true;
                }

                if (alreadyBlocked) {
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Уже в черном списке", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                if (isApp) {
                    rules.blockedApps = appendToArray(rules.blockedApps, target);
                } else {
                    rules.blockedDomains = appendToArray(rules.blockedDomains, target);
                }

                deviceRepo.updateDeviceRules(deviceId, rules, new DeviceRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                String typeText = isApp ? "Приложение" : "Сайт";
                                Toast.makeText(getContext(), typeText + " \"" + target + "\" заблокировано", Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Ошибка сохранения: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Не удалось загрузить настройки", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private String[] appendToArray(String[] array, String newValue) {
        if (array == null) {
            return new String[]{newValue};
        }
        String[] newArray = new String[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = newValue;
        return newArray;
    }

    private String calculateStartDate() {
        TimeZone moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow");
        Calendar cal = Calendar.getInstance(moscowTimeZone);
        cal.add(Calendar.DAY_OF_YEAR, -30);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(moscowTimeZone);

        return sdf.format(cal.getTime());
    }

    private enum GlobalState {
        LOADING,
        EMPTY,
        CONTENT
    }

    private void showGlobalState(GlobalState state) {
        if (layoutLoading != null) {
            layoutLoading.setVisibility(state == GlobalState.LOADING ? View.VISIBLE : View.GONE);
        }
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(state == GlobalState.EMPTY ? View.VISIBLE : View.GONE);
        }
        if (layoutContent != null) {
            layoutContent.setVisibility(state == GlobalState.CONTENT ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        recyclerView = null;
        chipGroup = null;
        btnLoadMore = null;
        editSearch = null;
        layoutLoading = layoutEmpty = layoutContent = layoutEmptyFilter = null;
        adapter = null;
    }
}