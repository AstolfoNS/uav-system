package com.tf.backend.core.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperCustomizer() {
        return builder -> {
            // 模块：Long -> String
            SimpleModule longToStringModule = new SimpleModule();
            longToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
            longToStringModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

            builder
                    .findAndAddModules()                                        // 自动注册其他模块（如日期/time 支持）
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)     // key 排序
                    .enable(SerializationFeature.INDENT_OUTPUT)                 // 美化输出
                    .addModule(longToStringModule);                             // 注册自定义模块
        };
    }
}
