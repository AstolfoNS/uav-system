package com.tf.backend.core.domain.auth;

import com.tf.backend.core.model.dto.LoginRequest;
import com.tf.backend.core.model.dto.TokenResponse;

public interface LoginService {
    TokenResponse login(LoginRequest request);

    void logout(Long userId, String refreshToken);
}
