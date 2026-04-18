package com.tf.backend.core.application.domain.auth;

import com.tf.backend.core.model.dto.LoginRequestDTO;
import com.tf.backend.core.model.dto.TokenResponseDTO;

public interface LoginService {
    TokenResponseDTO login(LoginRequestDTO request);

    void logout(Long userId, String refreshToken);
}
