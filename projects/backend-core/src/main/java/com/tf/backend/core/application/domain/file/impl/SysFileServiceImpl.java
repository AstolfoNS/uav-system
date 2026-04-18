package com.tf.backend.core.application.domain.file.impl;

import com.tf.backend.core.application.domain.file.SysFileService;
import com.tf.backend.core.application.infrastructure.oss.MinioTemplate;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.util.IdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SysFileServiceImpl implements SysFileService {

    private final MinioTemplate minioTemplate;

    // 头像允许的格式
    private static final List<String> ALLOWED_AVATAR_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    // 头像最大限制 2MB
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;


    @Override
    public String uploadFile(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        return doUpload(file, StringUtils.hasText(dir) ? dir : "common");
    }


    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传头像不能为空");
        }

        // 严格的业务校验
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException("头像文件大小不能超过 2MB");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new BizException("仅支持 JPG/PNG 格式的图片");
        }

        return doUpload(file, "avatar");
    }

    @Override
    public void deleteFileByUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        try {
            MinioTemplate.MinioBucket bucket = minioTemplate.bucket();
            String objectName = minioTemplate.extractObjectName(fileUrl, bucket.getBucketName());
            bucket.deleteFile(objectName);
        } catch (Exception e) {
            log.warn("跳过删除头像文件，URL 可能不属于当前 MinIO: {}", fileUrl);
            log.debug("删除头像文件异常详情", e);
        }
    }

    /**
     * 核心上传私有方法
     */
    private String doUpload(MultipartFile file, String dir) {
        try {
            MinioTemplate.MinioBucket bucket = minioTemplate.bucket();

            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 按天分目录：形如 yyyy/MM/dd
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

            // 最终拼接出的 objectName 形如：avatar/2026/04/17/uuid.jpg
            String objectName = dir + "/" + datePath + "/" + IdUtils.uuid() + extension;

            bucket.uploadFile(
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            return bucket.getPublicFileUrl(objectName);
        } catch (Exception e) {
            log.error("文件流处理异常: {}", e.getMessage(), e);

            throw new BizException("文件处理失败，请稍后重试");
        }
    }
}
