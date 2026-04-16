package com.tf.backend.core.model.dto;

import java.util.Map;

public record YoloNodeModelDTO(

        YoloNodeWeightsDTO weights,

        Map<String, Object> paramsMap
) {}
