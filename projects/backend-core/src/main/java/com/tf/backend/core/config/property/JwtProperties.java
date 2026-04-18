package com.tf.backend.core.config.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String key;

    private long expire = 86400000L;

    @NotNull
    private MacAlgorithm jwtAlgorithm = MacAlgorithm.HS256;

    @NotBlank
    private String jcaAlgorithm = "HmacSHA256";

    private long refreshExpire = 2592000000L;
}
