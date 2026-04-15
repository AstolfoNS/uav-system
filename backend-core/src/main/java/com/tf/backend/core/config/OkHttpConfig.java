package com.tf.backend.core.config;

import com.tf.backend.core.config.property.OkHttpProperties;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class OkHttpConfig {

    private final OkHttpProperties props;


    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(props.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(props.getReadTimeout()))
                .writeTimeout(Duration.ofSeconds(props.getWriteTimeout()))
                .build();
    }
}
