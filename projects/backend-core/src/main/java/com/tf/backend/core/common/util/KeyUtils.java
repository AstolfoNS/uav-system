package com.tf.backend.core.common.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class KeyUtils {

    private static final String DELIMITER = ":";


    private KeyUtils() {}

    /**
     * @param parts Key 的组成部分，可包含任意数量字段
     * @return 拼接好的 key 字符串
     * @throws IllegalArgumentException 当 parts 为空 或 全为无效字段
     */
    public static String of(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("Cache key 片段不能为空");
        }
        String key = Arrays.stream(parts)
                .filter(StringUtils::hasText)
                .map(String::trim).collect(Collectors.joining(DELIMITER));

        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Cache key 不能为空");
        }
        return key;
    }

    /**
     * @param prefix 模块前缀，例如 "user"
     * @param parts  其余 key 片段
     * @return 一个新的字符串数组，第一位为 prefix，其余为 parts
     */
    public static String[] withPrefix(String prefix, String... parts) {
        String[] combined = new String[parts.length + 1];

        combined[0] = prefix;

        System.arraycopy(parts, 0, combined, 1, parts.length);

        return combined;
    }

    public static final class KeyBuilder {

        private final List<String> parts = new ArrayList<>();


        private KeyBuilder(String first) {
            this.add(first);
        }

        /**
         * 启动 Key 构建器。
         * @param part 第一个 key 片段
         */
        public static KeyBuilder start(String part) {
            return new KeyBuilder(part);
        }

        /**
         * 添加一个字符串片段（自动 trim、忽略 null）。
         */
        public KeyBuilder add(String part) {
            if (StringUtils.hasText(part)) {
                parts.add(part.trim());
            }
            return this;
        }

        /**
         * 添加任意类型，如数字、UUID 等。
         * 自动调用 toString()。
         */
        public KeyBuilder add(Object part) {
            if (part != null) {
                add(part.toString());
            }
            return this;
        }

        /**
         * 生成最终 Key，例如："user:info:123"
         */
        public String build() {
            if (parts.isEmpty()) {
                throw new IllegalStateException("KeyBuilder 中没有任何 key 片段");
            }
            return String.join(DELIMITER, parts);
        }
    }
}
