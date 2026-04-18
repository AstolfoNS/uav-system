package com.tf.backend.core.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum TaskType {

    IMAGE(1),
    VIDEO(2);

    @JsonValue
    @EnumValue
    private final int code;

    private static final Map<Integer, TaskType> CODE_MAP = Stream
            .of(values())
            .collect(Collectors.toMap(TaskType::getCode, e -> e));


    public static TaskType fromCode(int code) {
        return CODE_MAP.getOrDefault(code, null);
    }
}
