package com.tf.backend.core.common.util;

import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class FileUtils {

    private FileUtils() {}

    /**
     * 提取文件扩展名（包含"."）
     */
    private static String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf(".");

        return (dotIndex >= 0) ? filename.substring(dotIndex) : "";
    }

    /**
     * 生成文件名：yyyyMMdd_UUID.xx
     */
    public static String generateFileObjectName(String originalFilename) {
        return "%s_%s%s".formatted(TimeUtils.todayCompact(), IdUtils.uuid(), getFileExtension(originalFilename));
    }

    /**
     * 生成文件完整路径：path/yyyy/MM/dd/uuid.xx
     */
    public static String generateFileObjectName(String originalFilename, String path) {
        return String.join("/", path, TimeUtils.todayPath(), IdUtils.uuid() + getFileExtension(originalFilename));
    }

    public static String encodeFilePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(p -> URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    /**
     * 检查 contentType 是否允许
     */
    public static boolean checkContentType(String contentType, Set<String> allowedContentTypes) {
        if (!StringUtils.hasText(contentType)) {
            throw new IllegalArgumentException("无法识别文件类型");
        }
        return allowedContentTypes.contains(contentType);
    }
}
