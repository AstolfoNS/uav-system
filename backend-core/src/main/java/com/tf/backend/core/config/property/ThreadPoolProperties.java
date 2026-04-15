package com.tf.backend.core.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {

    @Data
    public static class DbExecutor {

        private int corePoolSize = 4;

        private int maxPoolSize = 16;

        private int queueCapacity = 200;

        private int keepAliveSeconds = 60;

        @NotBlank
        private String threadNamePrefix = "db-async-";
    }

    private DbExecutor dbExecutor = new DbExecutor();

}
