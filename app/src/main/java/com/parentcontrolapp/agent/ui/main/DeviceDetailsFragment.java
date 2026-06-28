package com.parentcontrolapp.agent.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.Device;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;
import com.parentcontrolapp.agent.ui.adapters.DetailsPagerAdapter;

public class DeviceDetailsFragment extends Fragment {

    private DeviceRepository deviceRepo;
    private String deviceId;

    private TextView tvChild, tvDevice;
    private MaterialButton btnBlock, btnUnblock;
    private View overlayBlocked;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deviceRepo = new DeviceRepository(requireContext());

        Bundle args = getArguments();
        if (args == null || !args.containsKey("device_id")) {
            Toast.makeText(getContext(), "Ошибка: не передан device_id", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        deviceId = args.getString("device_id");

        String childName = args.getString("childName");
        String deviceName = args.getString("deviceName");

        initViews(view);

        if (childName != null) tvChild.setText(childName);
        if (deviceName != null) tvDevice.setText(deviceName);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        }

        setupBlockButtons();
        setupTabs();

        loadDeviceData();
    }

    private void initViews(View view) {
        tvChild = view.findViewById(R.id.tv_child_name);
        tvDevice = view.findViewById(R.id.tv_device_name);
        btnBlock = view.findViewById(R.id.btn_block);
        btnUnblock = view.findViewById(R.id.btn_unblock);
        overlayBlocked = view.findViewById(R.id.overlay_blocked);
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tabs_device);
    }

    private void loadDeviceData() {
        deviceRepo.fetchDeviceByCode(deviceId, new DeviceRepository.DeviceObjectCallback() {
            @Override
            public void onSuccess(Device device) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    bindDeviceData(device);
                    if (device.id != null) {
                        deviceId = device.getId();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Не удалось загрузить: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void bindDeviceData(Device device) {
        if (device.getChildName() != null) tvChild.setText(device.getChildName());
        if (device.getDeviceName() != null) tvDevice.setText(device.getDeviceName());
        updateBlockOverlay(device.isBlocked());
    }

    private void updateBlockOverlay(boolean isBlocked) {
        if (isBlocked) {
            overlayBlocked.setVisibility(View.VISIBLE);
            btnBlock.setVisibility(View.GONE);
        } else {
            overlayBlocked.setVisibility(View.GONE);
            btnBlock.setVisibility(View.VISIBLE);
        }
    }

    private void setupTabs() {
        DetailsPagerAdapter adapter = new DetailsPagerAdapter(requireActivity(), deviceId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Активность"); break;
                case 1: tab.setText("Журнал"); break;
                case 2: tab.setText("Скриншоты"); break;
                case 3: tab.setText("Настройки"); break;
            }
        }).attach();
    }

    private void setupBlockButtons() {
        btnBlock.setOnClickListener(v -> performBlock());

        btnUnblock.setOnClickListener(v -> performUnblock());
    }

    private void performBlock() {
        btnBlock.setEnabled(false);
        deviceRepo.toggleDeviceBlock(deviceId, true, new DeviceRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateBlockOverlay(true);
                    Toast.makeText(getContext(), "Устройство заблокировано", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnBlock.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performUnblock() {
        btnUnblock.setEnabled(false);
        deviceRepo.toggleDeviceBlock(deviceId, false, new DeviceRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateBlockOverlay(false);
                    btnUnblock.setEnabled(true);
                    Toast.makeText(getContext(), "Устройство разблокировано", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnUnblock.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btnBlock = btnUnblock = null;
        overlayBlocked = null;
        viewPager = null;
        tabLayout = null;
    }
}