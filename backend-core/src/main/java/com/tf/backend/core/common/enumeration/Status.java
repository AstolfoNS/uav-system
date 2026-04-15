package com.tf.backend.core.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum Status {

    DISABLED(0),
    ENABLED(1),
    LOCKED(2);

    @EnumValue
    private final int code;


    private static final Map<Integer, Status> CODE_MAP = Stream
            .of(values())
            .collect(Collectors.toMap(Status::getCode, e -> e));


    public static Status fromCode(int code) {
        return CODE_MAP.getOrDefault(code, null);
    }

}
