package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName(value = "yolo_node_params", autoResultMap = true)
public class YoloNodeParamEntity extends BaseEntity {

    private Long nodeId;

    private String templateName;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;

    private Boolean isActive;

}
