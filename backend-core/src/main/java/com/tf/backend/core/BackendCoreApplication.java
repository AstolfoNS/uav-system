package com.tf.backend.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan("com.tf.backend.core.config.property")
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class BackendCoreApplication {

    static void main(String[] args) {
        SpringApplication.run(BackendCoreApplication.class, args);
    }

}
