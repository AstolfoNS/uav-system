package com.tf.backend.core.common.util;

import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParserUtils {

    private DurationParserUtils() {}

    /**
     * 支持格式：
     * - "1500ms"
     * - "60s"
     * - "5m"
     * - "2h"
     * - "1d"
     * - "  60 s  "（允许空格）
     * - "60"（纯数字：默认秒）
     * 不支持：
     * - 小数（如 1.5m）
     * - 组合（如 1h30m）
     */
    private static final Pattern P = Pattern.compile("^\\s*(\\d+)\\s*(ms|s|m|h|d)?\\s*$", Pattern.CASE_INSENSITIVE);

    /**
     * @return 解析成功返回 Duration；失败返回 null
     */
    public static Duration parseOrNull(String input) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        Matcher m = P.matcher(input);
        if (!m.matches()) {
            return null;
        }
        String numStr = m.group(1);
        String unitStr = m.group(2);

        long n;
        try {
            n = Long.parseLong(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
        if (unitStr == null || unitStr.isBlank()) {
            // 纯数字默认秒
            return Duration.ofSeconds(n);
        }
        String unit = unitStr.toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "ms" -> Duration.ofMillis(n);
            case "s"  -> Duration.ofSeconds(n);
            case "m"  -> Duration.ofMinutes(n);
            case "h"  -> Duration.ofHours(n);
            case "d"  -> Duration.ofDays(n);
            default   -> null;
        };
    }

    /**
     * 解析失败直接抛异常（适合命令参数校验）
     */
    public static Duration parseOrThrow(String input) {
        Duration d = parseOrNull(input);
        if (d == null) {
            throw new IllegalArgumentException("Invalid duration: " + input + " (supported: ms/s/m/h/d, e.g. 1500ms, 60s, 5m, 2h, 1d)");
        }
        return d;
    }
}
