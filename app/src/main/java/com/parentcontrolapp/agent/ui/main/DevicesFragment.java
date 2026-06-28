package com.parentcontrolapp.agent.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.Device;
import com.parentcontrolapp.agent.data.repository.AuthRepository;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;
import com.parentcontrolapp.agent.ui.adapters.DeviceAdapter;
import com.parentcontrolapp.agent.ui.adapters.DeviceItem;

import java.util.ArrayList;
import java.util.List;

public class DevicesFragment extends Fragment {

    private AuthRepository authRepo;
    private DeviceRepository deviceRepo;

    private RecyclerView recycler;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fab;
    private SwipeRefreshLayout swipeRefresh;
    private DeviceAdapter adapter;
    private static final String TAG = "DevicesFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo = new AuthRepository(requireContext());
        deviceRepo = new DeviceRepository(requireContext());

        initViews(view);
        setupListeners();
        loadDevices();
    }

    private void initViews(View view) {
        recycler = view.findViewById(R.id.recycler_devices);
        shimmerLayout = view.findViewById(R.id.shimmer_layout);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        fab = view.findViewById(R.id.fab_add);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setHasFixedSize(true);

        adapter = new DeviceAdapter(
                new ArrayList<>(),
                this::navigateToDeviceDetails,
                this::showDeviceOptionsDialog
        );
        recycler.setAdapter(adapter);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.green_600);
        }
    }


    private void setupListeners() {
        fab.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.addDeviceFragment);
            } catch (Exception e) {
                Log.e(TAG, "Navigation error", e);
            }
        });

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadDevices);
        }
    }

    private void navigateToDeviceDetails(DeviceItem deviceItem) {
        Bundle args = new Bundle();
        args.putString("device_id", deviceItem.getId());
        args.putString("childName", deviceItem.getChildName());
        args.putString("deviceName", deviceItem.getDeviceName());

        try {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_devices_to_details, args);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Ошибка навигации", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeviceOptionsDialog(DeviceItem device, View anchorView) {
        if (!isAdded()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_device_options, null);

        TextView tvTitle = sheetView.findViewById(R.id.tv_title);
        TextView tvSubtitle = sheetView.findViewById(R.id.tv_subtitle);
        tvTitle.setText(device.getChildName());
        tvSubtitle.setText(device.getDeviceName());

        sheetView.findViewById(R.id.btn_open).setOnClickListener(v -> {
            bottomSheet.dismiss();
            navigateToDeviceDetails(device);
        });

        sheetView.findViewById(R.id.btn_unlink).setOnClickListener(v -> {
            bottomSheet.dismiss();
            confirmUnlinkDevice(device);
        });

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void confirmUnlinkDevice(DeviceItem device) {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Отвязать устройство?")
                .setMessage("Устройство \"" + device.getChildName() + "\" будет отвязано от вашего аккаунта. Ребёнок сможет использовать устройство без ограничений.")
                .setPositiveButton("Отвязать", (dialog, which) -> {
                    performUnlinkDevice(device);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performUnlinkDevice(DeviceItem device) {
        if (!isAdded()) return;

        setLoading(true);

        deviceRepo.unlinkDevice(device.getId(), new DeviceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Устройство отвязано", Toast.LENGTH_SHORT).show();
                    loadDevices();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Загружает список устройств текущего пользователя из Supabase
     */
    private void loadDevices() {
        setLoading(true);

        String userId = authRepo.getCurrentUserId();
        if (userId == null) {
            setLoading(false);
            showError("Пользователь не авторизован");
            showEmptyState();
            return;
        }

        deviceRepo.fetchDevices(userId, new DeviceRepository.DeviceListCallback() {
            @Override
            public void onSuccess(List<Device> devices) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);

                    if (devices != null && !devices.isEmpty()) {
                        showDevices(devices);
                    } else {
                        showEmptyState();
                    }
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showEmptyState();
                });
            }
        });
    }

    /**
     * Отображает список устройств в RecyclerView
     */
    private void showDevices(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            showEmptyState();
            return;
        }

        List<DeviceItem> items = new ArrayList<>();
        for (Device device : devices) {
            if (device.id == null) {
                Log.w(TAG, "Skipping device with null id");
                continue;
            }

            items.add(new DeviceItem(
                    device.id,
                    device.childName != null ? device.childName : "Неизвестно",
                    device.deviceName != null ? device.deviceName : "Устройство",
                    device.deviceType != null ? device.deviceType : "windows",
                    device.isActive,
                    device.isOnline(),
                    device.isBlocked
            ));
        }

        adapter.updateData(items);

        shimmerLayout.setVisibility(View.GONE);
        if (swipeRefresh != null) {
            swipeRefresh.setVisibility(View.VISIBLE);
        }
        layoutEmpty.setVisibility(View.GONE);
    }

    /**
     * Показывает пустое состояние (нет устройств)
     */
    private void showEmptyState() {
        shimmerLayout.setVisibility(View.GONE);
        if (swipeRefresh != null) {
            swipeRefresh.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    /**
     * Показывает/скрывает индикатор загрузки
     */
    private void setLoading(boolean loading) {
        if (loading) {
            shimmerLayout.startShimmer();
            shimmerLayout.setVisibility(View.VISIBLE);
            if (swipeRefresh != null) {
                swipeRefresh.setVisibility(View.GONE);
            }
            layoutEmpty.setVisibility(View.GONE);
            fab.setEnabled(false);
            if (swipeRefresh != null) {
                swipeRefresh.setEnabled(false);
            }
        } else {
            shimmerLayout.stopShimmer();
            fab.setEnabled(true);
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
                swipeRefresh.setEnabled(true);
            }
        }
    }

    /**
     * Показывает ошибку пользователю
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}