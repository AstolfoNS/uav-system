package com.tf.backend.core.common.util;

import java.util.UUID;

public final class IdUtils {

    private IdUtils() {}

    /**
     * 生成一个无 "-" 的 UUID（32 位十六进制）
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成标准 UUID（36 位，含 "-"）
     */
    public static String uuid32() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成一个前缀ID，例如 user_xxxx、file_xxxx
     */
    public static String prefixed(String prefix) {
        return prefix + "_" + uuid();
    }
}
