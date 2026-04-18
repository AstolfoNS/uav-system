package com.tf.backend.core.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    @NotBlank
    private String publicBaseUrl;

    @NotBlank
    private String endpoint;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String defaultBucket;

    private int presignedUrlExpiry = 3600;

    private int partSize = 5242880;

    @Data
    public static class FileExecutor {

        private int corePoolSize = 2;

        private int maxPoolSize = 4;

        private int queueCapacity = 200;

        private int keepAliveSeconds = 60;

        @NotBlank
        private String threadNamePrefix = "minio-file-";

        private int awaitTerminationSeconds = 60;
    }

    private FileExecutor fileExecutor = new FileExecutor();
}

