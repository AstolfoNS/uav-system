package com.tf.backend.core.common.util;

public class UrlUtils {

    /**
     * 安全地拼接 URL，防止多出或遗漏斜杠
     */
    public static String buildUrl(String baseUrl, String path) {
        boolean baseEndsWithSlash = baseUrl.endsWith("/");
        boolean pathStartsWithSlash = path.startsWith("/");

        if (baseEndsWithSlash && pathStartsWithSlash) {
            return baseUrl + path.substring(1);
        } else if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return baseUrl + "/" + path;
        } else {
            return baseUrl + path;
        }
    }
}
