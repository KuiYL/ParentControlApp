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
import com.parentcontrolapp.agent.data.remote.SupabaseApi;

public class ForgotPasswordFragment extends Fragment {

    private TextInputEditText etEmail;
    private MaterialButton btnSubmit;
    private View btnBack, txtBackToLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClicks();
    }

    private void initViews(View view){
        etEmail = view.findViewById(R.id.et_email);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnBack = view.findViewById(R.id.btn_back);
        txtBackToLogin = view.findViewById(R.id.txt_back_to_login);
    }

    private void setupClicks(){
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        txtBackToLogin.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_forgot_password_to_login));

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Введите email");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Введите корректный email");
                return;
            }

            setLoading(true);

            SupabaseApi.getInstance(requireContext()).recoverPassword(email, new SupabaseApi.SimpleCallback() {
                @Override
                public void onSuccess() {
                    requireActivity().runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(getContext(), "Если аккаунт существует, инструкция отправлена на: " + email, Toast.LENGTH_LONG).show();
                        etEmail.setText("");
                        NavHostFragment.findNavController(ForgotPasswordFragment.this).popBackStack();
                    });
                }

                @Override
                public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        setLoading(false);

                        String errorMsg = message;
                        if (message.contains("401") || message.contains("Secret API key")) {
                            errorMsg = "Функция восстановления требует настройки сервера";
                        } else if (message.contains("User not found")) {
                            errorMsg = "Пользователь с таким email не найден";
                        }

                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Отправка..." : "Отправить инструкцию");
        etEmail.setEnabled(!loading);
    }
}
