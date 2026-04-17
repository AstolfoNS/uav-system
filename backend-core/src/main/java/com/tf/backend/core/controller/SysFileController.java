package com.tf.backend.core.controller;

import com.tf.backend.core.application.domain.file.SysFileService;
import com.tf.backend.core.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/files")
@RestController
@Tag(name = "系统级文件管理", description = "统一处理文件流上传，底层对接 MinIO")
public class SysFileController {

    private final SysFileService sysFileService;


    @PostMapping("/upload")
    @Operation(summary = "通用文件上传", description = "无严格校验，可指定分类目录")
    public R<String> uploadFile(

            @RequestParam("file")
            MultipartFile file,

            @RequestParam(value = "dir", defaultValue = "common")
            String dir

    ) {
        String url = sysFileService.uploadFile(file, dir);

        return R.ok(url, "文件上传成功");
    }


    @PostMapping("/upload/avatar")
    @Operation(summary = "上传用户头像", description = "严格校验图片格式(JPG/PNG)和大小(最大2MB)")
    public R<String> uploadAvatar(

            @RequestParam("file")
            MultipartFile file

    ) {
        String url = sysFileService.uploadAvatar(file);

        return R.ok(url, "头像上传成功");
    }


}