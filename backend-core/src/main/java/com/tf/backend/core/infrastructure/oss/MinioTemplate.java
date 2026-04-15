package com.tf.backend.core.infrastructure.oss;

import com.tf.backend.core.config.property.MinioProperties;
import com.tf.backend.core.common.exception.MinioFileUrlExtractException;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class MinioTemplate {

    private final MinioClient minioClient;

    private final MinioProperties props;

    private final Executor minioFileExecutor;


    public MinioBucket bucket(String bucketName) {
        validateBucketName(bucketName);
        return new MinioBucket(bucketName, props.getPresignedUrlExpiry(), props.getPartSize());
    }

    public boolean bucketExists(String bucketName) {
        validateBucketName(bucketName);
        return execute(
                () -> minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName).build()
                ),
                "检查桶是否存在失败: %s".formatted(bucketName)
        );
    }

    /**
     * 解析 URL 获取真正的 ObjectName
     */
    public String extractObjectName(String fileUrl, String bucketName) {
        if (fileUrl == null || bucketName == null) {
            throw new IllegalArgumentException("fileUrl 或 bucketName 不能为空");
        }
        try {
            // 解码并清洗路径
            return sanitizeObjectName(UriUtils.decode(getRawObjectName(fileUrl, bucketName), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new MinioFileUrlExtractException("解析 MinIO 对象路径失败: %s".formatted(fileUrl), e);
        }
    }

    /**
     * 剔除 URL 中可能携带的参数 (如预签名 URL 的 ?X-Amz-...)
     */
    private @NonNull String getRawObjectName(String fileUrl, String bucketName) {
        int queryIndex = fileUrl.indexOf('?');
        String urlWithoutQuery = queryIndex > -1 ? fileUrl.substring(0, queryIndex) : fileUrl;

        String prefixWithGateway = "%s/%s/".formatted(props.getPublicBaseUrl(), bucketName);
        if (!urlWithoutQuery.startsWith(prefixWithGateway)) {
            throw new MinioFileUrlExtractException("fileUrl 不符合预期格式: %s".formatted(fileUrl));
        }
        // 截取真实的对象路径
        return urlWithoutQuery.substring(prefixWithGateway.length());
    }

    private <T> T execute(MinioAction<T> action, String errorMessage) {
        try {
            return action.run();
        } catch (Exception e) {
            throw new MinioOperationException(errorMessage, e);
        }
    }

    private void validateBucketName(String bucketName) {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalArgumentException("桶名不能为空");
        }
    }

    /**
     * 清洗对象名称 (处理异常目录结构)
     */
    private String sanitizeObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("对象名/路径不能为空");
        }
        return objectName.replace("\\", "/")    // 替换反斜杠为正斜杠 (兼容 Windows 传参习惯)
                .replaceAll("^/+", "")          // 去除最前面的斜杠 (防止变成空根目录)
                .replaceAll("/+", "");          // 将连续的多个斜杠合并为一个
    }

    @FunctionalInterface
    private interface MinioAction<T> {
        T run() throws Exception;
    }

    public static class MinioOperationException extends RuntimeException {
        public MinioOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Getter
    public class MinioBucket {

        private final String bucketName;

        private final int presignedUrlExpiry;

        private final int partSize;

        public MinioBucket(String bucketName, int presignedUrlExpiry, int partSize) {
            this.bucketName = bucketName;
            this.presignedUrlExpiry = presignedUrlExpiry;
            this.partSize = partSize;
        }

        // ---------------------------
        // Upload (自动支持创建多级目录)
        // ---------------------------

        public void uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
            String cleanObjName = sanitizeObjectName(objectName);
            if (inputStream == null) {
                throw new IllegalArgumentException("输入流不能为空");
            }
            if (size < 0) {
                throw new IllegalArgumentException("size 必须 >= 0");
            }
            execute(() -> {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(cleanObjName)
                                .stream(inputStream, size, partSize)
                                .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                                .build()
                );
                log.info("文件上传成功: {}/{}", bucketName, cleanObjName);
                return null;
            }, "上传文件失败: %s".formatted(cleanObjName));
        }

        public void uploadFile(String objectName, byte[] content, String contentType) {
            if (content == null) {
                throw new IllegalArgumentException("content 不能为空");
            }
            try (InputStream is = new ByteArrayInputStream(content)) {
                uploadFile(objectName, is, content.length, contentType);
            } catch (IOException e) {
                throw new MinioOperationException("byte[] 上传失败: " + objectName, e);
            }
        }

        public void uploadFile(String objectName, File file, String contentType) {
            try (InputStream is = new FileInputStream(file)) {
                uploadFile(objectName, is, Files.size(file.toPath()), contentType);
            } catch (IOException e) {
                throw new MinioOperationException("File 上传失败: " + objectName, e);
            }
        }

        public CompletableFuture<Void> uploadFileAsync(String objectName, byte[] content, String contentType) {
            return CompletableFuture.runAsync(() -> uploadFile(objectName, content, contentType), minioFileExecutor);
        }

        // ---------------------------
        // Download
        // ---------------------------

        public InputStream downloadAsStream(String objectName) {
            String cleanObjName = sanitizeObjectName(objectName);
            return execute(() -> minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(cleanObjName)
                    .build()), "下载文件失败: " + cleanObjName);
        }

        public void downloadFile(String objectName, Consumer<InputStream> consumer) {
            String cleanObjName = sanitizeObjectName(objectName);
            execute(() -> {
                try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(cleanObjName)
                        .build())) {
                    consumer.accept(stream);
                }
                return null;
            }, "下载文件失败: " + cleanObjName);
        }

        public byte[] downloadAsBytes(String objectName) {
            try (InputStream stream = downloadAsStream(objectName)) {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new MinioOperationException("下载为 bytes 失败: " + objectName, e);
            }
        }

        public void downloadToFile(String objectName, File dest) {
            try (InputStream stream = downloadAsStream(objectName);
                 OutputStream os = new FileOutputStream(dest)) {
                stream.transferTo(os);
            } catch (IOException e) {
                throw new MinioOperationException("下载到本地失败: " + objectName, e);
            }
        }

        public CompletableFuture<Void> downloadFileAsync(String objectName, Consumer<InputStream> consumer) {
            return CompletableFuture.runAsync(() -> downloadFile(objectName, consumer), minioFileExecutor);
        }

        // ---------------------------
        // Delete / Exists
        // ---------------------------

        public void deleteFile(String objectName) {
            String cleanObjName = sanitizeObjectName(objectName);
            execute(() -> {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(cleanObjName)
                                .build()
                );
                return null;
            }, "删除文件失败: " + cleanObjName);
        }

        public boolean objectExists(String objectName) {
            String cleanObjName = sanitizeObjectName(objectName);
            return execute(() -> {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(cleanObjName)
                                .build()
                );
                return true;
            }, "对象不存在或检查失败: " + cleanObjName);
        }

        // ---------------------------
        // Copy / Move / List
        // ---------------------------

        public void copy(String sourceObjectName, String targetObjectName) {
            String cleanSource = sanitizeObjectName(sourceObjectName);
            String cleanTarget = sanitizeObjectName(targetObjectName);

            execute(() -> {
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(cleanTarget)
                                .source(
                                        CopySource.builder()
                                                .bucket(bucketName)
                                                .object(cleanSource)
                                                .build()
                                )
                                .build()
                );
                return null;
            }, "拷贝对象失败: %s -> %s".formatted(cleanSource, cleanTarget));
        }

        public void move(String sourceObjectName, String targetObjectName) {
            copy(sourceObjectName, targetObjectName);
            deleteFile(sourceObjectName);
        }

        /**
         * 列出目录下所有文件
         * @param prefix 目录前缀 (例如 "users/avatar/")
         * @param recursive true=递归查出子孙目录所有文件, false=只查当前层级
         */
        public List<String> listObjects(String prefix, boolean recursive) {
            // 如果前缀不为空，且不是以 / 结尾，可以考虑补齐（可选，视具体业务逻辑而定）
            String cleanPrefix = StringUtils.hasText(prefix) ? sanitizeObjectName(prefix) : "";

            return execute(() -> {
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(cleanPrefix)
                                .recursive(recursive)
                                .build()
                );
                List<String> list = new ArrayList<>();
                for (Result<Item> r : results) {
                    list.add(r.get().objectName());
                }
                return list;
            }, "列举对象失败: " + cleanPrefix);
        }

        // 重载默认全量查
        public List<String> listObjects(String prefix) {
            return listObjects(prefix, true);
        }

        // ---------------------------
        // Public URL / Presigned
        // ---------------------------

        /**
         * 获取公开 URL
         */
        public String getPublicFileUrl(String objectName) {
            // 使用 UriComponentsBuilder，按 '/' 拆分并逐级添加，这样会自动对每一级(如空格、中文)进行编码，但保留 '/'
            return UriComponentsBuilder.fromUriString(props.getPublicBaseUrl())
                    .pathSegment(bucketName)
                    .pathSegment(sanitizeObjectName(objectName).split("/"))
                    .build()
                    .toUriString();
        }

        public String getPresignedGetUrl(String objectName) {
            return getPresignedUrl(objectName, Method.GET, presignedUrlExpiry);
        }

        public String getPresignedPutUrl(String objectName) {
            return getPresignedUrl(objectName, Method.PUT, presignedUrlExpiry);
        }

        private String getPresignedUrl(String objectName, Method method, long expirySeconds) {
            String cleanObjName = sanitizeObjectName(objectName);
            return execute(() -> minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(cleanObjName)
                            .method(method)
                            .expiry((int) expirySeconds)
                            .build()
            ), "生成预签名 URL 失败: " + cleanObjName);
        }
    }
}
