package com.tf.backend.core.application.security;

import com.tf.backend.core.common.util.IdUtils;
import com.tf.backend.core.config.property.JwtProperties;
import com.tf.backend.core.common.exception.JwtGenerateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    @Getter
    private final JwtProperties props;

    /**
     * 生成 Access Token (短效，无 jti)
     */
    public String generateAccessToken(Long userId) {
        return generateToken(userId, props.getExpire(), null);
    }

    /**
     * 生成 Refresh Token (动态决定寿命)
     */
    public String generateRefreshToken(Long userId, boolean rememberMe) {
        // 如果记住我，使用配置的长效时间（如 30天）；否则使用短效时间（如 12小时）
        return generateToken(userId, rememberMe ? props.getRefreshExpire() : java.time.Duration.ofHours(12).toMillis(), IdUtils.uuid());
    }

    /**
     * 底层通用生成逻辑
     */
    private String generateToken(Long userId, long expireMillis, String jti) {
        Instant now = Instant.now();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .issuer(props.getIssuer())
                .expiresAt(now.plusMillis(expireMillis));

        // 如果是 Refresh Token，塞入 jti 声明
        if (StringUtils.hasText(jti)) {
            builder.claim("jti", jti);
        }
        JwsHeader jwsHeader = JwsHeader.with(props.getJwtAlgorithm()).build();
        try {
            return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
        } catch (JwtEncodingException e) {
            throw new JwtGenerateException("生成 JWT token 失败", e);
        }
    }

    public Jwt decode(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtException("token 不能为空");
        }
        return jwtDecoder.decode(token);
    }

    public Long getUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            log.error("无法从 Token 中解析出 User ID: {}", jwt.getSubject());
            return null;
        }
    }

    public Long getUserId(String token) {
        return getUserId(decode(token));
    }

    /**
     * 提取 Refresh Token 的唯一标识 jti
     */
    public String getJti(Jwt jwt) {
        if (jwt == null) return null;
        return jwt.getClaimAsString("jti");
    }
}
