package com.tf.backend.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "认证凭证响应结果")
public record TokenResponse(

        @Schema(description = "访问令牌 (Access Token)，用于请求业务接口，寿命较短")
        String accessToken,

        @Schema(description = "刷新令牌 (Refresh Token)，用于在 AT 过期时换取新 Token，寿命较长")
        String refreshToken,

        @Schema(description = "访问令牌的有效时间（单位：秒）", example = "900")
        long expiresIn
) {
}
