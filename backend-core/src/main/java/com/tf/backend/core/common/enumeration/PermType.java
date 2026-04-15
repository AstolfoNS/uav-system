package com.tf.backend.core.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum PermType {

    API(1),
    WEB(2),
    BOT(3);

    @EnumValue
    private final int code;

    private static final Map<Integer, PermType> CODE_MAP = Stream
            .of(values())
            .collect(Collectors.toMap(PermType::getCode, e -> e));


    public static PermType fromCode(int code) {
        return CODE_MAP.getOrDefault(code, null);
    }

}
