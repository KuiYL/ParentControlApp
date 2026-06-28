package com.parentcontrolapp.agent.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.repository.AuthRepository;
import com.parentcontrolapp.agent.data.repository.DeviceRepository;

public class AddDeviceFragment extends Fragment {

    private AuthRepository authRepo;
    private DeviceRepository deviceRepo;

    private TextInputEditText etCode;
    private MaterialButton btnClaim;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo = new AuthRepository(requireContext());
        deviceRepo = new DeviceRepository(requireContext());

        etCode = view.findViewById(R.id.et_pairing_code);
        btnClaim = view.findViewById(R.id.btn_claim);
        View btnBack = view.findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack()
        );

        btnClaim.setOnClickListener(v -> claimDevice());
    }

    /**
     * Обработчик привязки устройства по коду
     */
    private void claimDevice() {
        String code = etCode.getText().toString();
        String parentEmail = authRepo.getCurrentUserEmail();

        if (code.isEmpty()) {
            etCode.setError("Введите код");
            etCode.requestFocus();
            return;
        }
        if (parentEmail == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        deviceRepo.claimDevice(code, parentEmail, new DeviceRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Устройство привязано!", Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(AddDeviceFragment.this).popBackStack();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);

                    String errorMsg = message;
                    if (message.contains("Invalid or expired")) {
                        errorMsg = "Неверный или истёкший код";
                    } else if (message.contains("Parent account not found")) {
                        errorMsg = "Аккаунт не найден";
                    }

                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Блокировка/разблокировка UI во время загрузки
     */
    private void setLoading(boolean loading) {
        btnClaim.setEnabled(!loading);
        btnClaim.setText(loading ? "Привязка..." : "Привязать");
        etCode.setEnabled(!loading);
    }
}