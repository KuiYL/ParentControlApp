package com.parentcontrolapp.agent.ui.auth;

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

import java.util.Objects;

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;

    private AuthRepository authRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo = new AuthRepository(requireContext());

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin = view.findViewById(R.id.btn_login);
        View txtRegister = view.findViewById(R.id.txt_register);
        View txtForgot = view.findViewById(R.id.txt_forgot);

        btnLogin.setOnClickListener(v -> performLogin());

        txtRegister.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));
        txtForgot.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_login_to_forgot_password));
    }

    private void performLogin() {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String pass = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        authRepo.login(email, pass, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    NavHostFragment.findNavController(LoginFragment.this).navigate(R.id.action_login_to_main);
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Вход..." : "Войти");
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
}