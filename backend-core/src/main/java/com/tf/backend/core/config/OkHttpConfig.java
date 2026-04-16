package com.tf.backend.core.config;

import com.tf.backend.core.config.property.OkHttpProperties;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class OkHttpConfig {

    private final OkHttpProperties props;


    @Primary
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(props.getConnectTimeout()))
                .readTimeout(Duration.ofSeconds(props.getReadTimeout()))
                .writeTimeout(Duration.ofSeconds(props.getWriteTimeout()))
                .connectionPool(new okhttp3.ConnectionPool(props.getConnectPoolMaxIdleConnections(), props.getConnectPollKeepAliveDuration(), TimeUnit.MINUTES))
                .build();
    }

    @Bean("inferenceOkHttpClient")
    public OkHttpClient inferenceOkHttpClient() {
        return new OkHttpClient.Builder()
                // 连接超时依然保持短一点，如果物理机挂了能立刻发现
                .connectTimeout(10, TimeUnit.SECONDS)
                // 上传视频可能很慢，写入超时设为 5 分钟
                .writeTimeout(5, TimeUnit.MINUTES)
                // 深度学习推理最耗时，读取超时设为 10 分钟
                .readTimeout(10, TimeUnit.MINUTES)
                .build();
    }

}
