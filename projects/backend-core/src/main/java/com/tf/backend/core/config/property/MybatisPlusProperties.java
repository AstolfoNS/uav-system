package com.tf.backend.core.config.property;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "mybatis-plus")
public class MybatisPlusProperties {

    private DbType dbType = DbType.MYSQL;

}
