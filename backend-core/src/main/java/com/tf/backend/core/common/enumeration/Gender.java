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
public enum Gender {

    UNKNOWN(0),
    MALE(1),
    FEMALE(2);

    @JsonValue
    @EnumValue
    private final int code;

    private static final Map<Integer, Gender> CODE_MAP = Stream
            .of(values())
            .collect(Collectors.toMap(Gender::getCode, e -> e));


    public static Gender fromCode(int code) {
        return CODE_MAP.getOrDefault(code, null);
    }

}
