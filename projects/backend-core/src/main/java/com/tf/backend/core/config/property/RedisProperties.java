package com.tf.backend.core.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    private long defaultCacheTTLHours = 1;

}
