package com.tf.backend.core.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tf.backend.core.model.entity.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URL;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TableName("yolo_nodes")
public class YoloNodeEntity extends BaseEntity {

    private String nodeName;

    private String description;

    private String host;

    private String port;

    private String httpProtocol;

    private String apiVersion;

    private String activeWeightName;


    public String getBaseUrl() {
        return String.format("%s://%s:%s/api/%s", httpProtocol, host, port, apiVersion);
    }

}
