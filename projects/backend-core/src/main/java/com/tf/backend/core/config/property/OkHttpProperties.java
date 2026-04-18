package com.tf.backend.core.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "okhttp")
public class OkHttpProperties {

    private int connectTimeout = 10;

    private int readTimeout = 30;

    private int writeTimeout = 30;

    private int connectPoolMaxIdleConnections = 50;

    private long connectPollKeepAliveDuration = 5;

}
