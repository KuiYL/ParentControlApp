package com.parentcontrolapp.agent.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parentcontrolapp.agent.MainActivity;
import com.parentcontrolapp.agent.R;
import com.parentcontrolapp.agent.data.model.Profile;
import com.parentcontrolapp.agent.data.repository.AuthRepository;

public class ProfileFragment extends Fragment {

    private AuthRepository authRepo;
    private TextView tvParentName, tvEmail;
    private View btnAbout, btnLogout;

    private boolean isProfileLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo = new AuthRepository(requireContext());
        initViews(view);
        loadProfile();
        setupClickListeners();
    }

    private void initViews(View view) {
        tvParentName = view.findViewById(R.id.tv_parent_name);
        tvEmail = view.findViewById(R.id.tv_email);
        btnAbout = view.findViewById(R.id.btn_about);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void loadProfile() {
        if (isProfileLoading) return;
        isProfileLoading = true;

        displayFallbackProfile();

        String userId = authRepo.getCurrentUserId();
        if (userId == null) {
            isProfileLoading = false;
            return;
        }

        authRepo.getProfile(userId, new AuthRepository.ProfileCallback() {
            @Override
            public void onSuccess(Profile profile) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        displayProfile(profile);
                        isProfileLoading = false;
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    isProfileLoading = false;
                }
            }
        });
    }

    private void displayProfile(Profile profile) {
        if (!isAdded()) return;

        tvParentName.setText(profile.getFullName() != null && !profile.getFullName().isEmpty()
                ? profile.getFullName() : "Пользователь");
        tvEmail.setText(profile.getEmail() != null ? profile.getEmail() : "—");
    }

    private void displayFallbackProfile() {
        if (!isAdded()) return;

        String email = authRepo.getCurrentUserEmail();
        tvParentName.setText("Пользователь");
        tvEmail.setText(email != null ? email : "—");
    }

    private void setupClickListeners() {
        btnAbout.setOnClickListener(v -> showAboutDialog());
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void showAboutDialog() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("О приложении")
                .setMessage("ParentControl v1.0.0\n\nПриложение для родительского контроля.\n© 2026")
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmLogout() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти", (dialog, which) -> performLogout())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performLogout() {
        authRepo.logout(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        authRepo.clearSessionForced();
                        Toast.makeText(getContext(), "Выход выполнен", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    });
                }
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}