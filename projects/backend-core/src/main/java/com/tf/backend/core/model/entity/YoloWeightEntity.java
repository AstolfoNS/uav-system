package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName("yolo_weights")
public class YoloWeightEntity extends BaseEntity {

    private Long nodeId;

    private String filename;

    private String description;

    private Boolean isActive;

}
