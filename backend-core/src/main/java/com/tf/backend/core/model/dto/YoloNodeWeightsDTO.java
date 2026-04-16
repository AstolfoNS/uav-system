package com.tf.backend.core.model.dto;

import java.util.List;

public record YoloNodeWeightsDTO(

        String activeWeight,

        List<String> availableWeights
) {}
