package com.tf.backend.core.model.vo;

import com.tf.backend.core.common.enumeration.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record YoloNodeDetailVO(

        Long id,

        String nodeName,

        String description,

        String host,

        String port,

        String activeWeightName,

        Status status,

        LocalDateTime updatedAt,
        
        List<String> availableWeights,

        Map<String, Object> currentParams
) {}