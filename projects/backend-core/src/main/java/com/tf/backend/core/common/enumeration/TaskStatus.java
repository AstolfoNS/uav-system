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
public enum TaskStatus {

    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3);

    @JsonValue
    @EnumValue
    private final int code;

    private static final Map<Integer, TaskStatus> CODE_MAP = Stream
            .of(values())
            .collect(Collectors.toMap(TaskStatus::getCode, e -> e));

    public static TaskStatus fromCode(int code) {
        return CODE_MAP.getOrDefault(code, null);
    }
}
