package com.tf.backend.core.application.domain.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 系统文件服务接口 (负责业务校验、分类存放)
 */
public interface SysFileService {

    /**
     * 通用文件上传 (无严格格式限制)
     * @param file 文件
     * @param dir 指定存放目录
     * @return 访问 URL
     */
    String uploadFile(MultipartFile file, String dir);

    /**
     * 头像专属上传 (有严格的图片格式和大小校验)
     * @param file 头像图片文件
     * @return 访问 URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 按公开访问 URL 删除文件（仅处理当前系统 MinIO URL）
     * @param fileUrl 公网访问 URL
     */
    void deleteFileByUrl(String fileUrl);
}