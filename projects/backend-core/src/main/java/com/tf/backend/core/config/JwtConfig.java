package com.tf.backend.core.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tf.backend.core.config.property.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties props;


    @Bean
    public SecretKeySpec secretKeySpec() {
        return new SecretKeySpec(props.getKey().getBytes(StandardCharsets.UTF_8), props.getJcaAlgorithm());
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKeySpec secretKeySpec) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKeySpec));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKeySpec secretKeySpec) {
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

}
