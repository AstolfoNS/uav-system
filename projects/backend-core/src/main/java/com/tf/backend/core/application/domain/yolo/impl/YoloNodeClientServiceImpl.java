package com.tf.backend.core.application.domain.yolo.impl;

import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.util.UrlUtils;
import com.tf.backend.core.application.domain.yolo.YoloNodeClientService;
import com.tf.backend.core.network.RequestSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoloNodeClientServiceImpl implements YoloNodeClientService {

    private final JsonMapper jsonMapper;

    private final RequestSender requestSender;

    // ================= 权重管理 =================

    @Override
    public void uploadWeight(String baseUrl, MultipartFile file) {
        String url = UrlUtils.buildUrl(baseUrl, "/weights/");
        try {
            RequestBody fileBody = RequestBody.create(
                    file.getBytes(),
                    MediaType.parse("application/octet-stream")
            );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            requestSender.executeRequestWithValidation(request, "上传权重文件失败");
        } catch (IOException e) {
            log.error("Failed to read multipart file", e);
            throw new BizException("读取上传文件失败");
        }
    }

    @Override
    public void changeActiveWeight(String baseUrl, String filename) {
        String url = UrlUtils.buildUrl(baseUrl, "/weights/" + filename + "/active");
        
        Request request = new Request.Builder()
                .url(url)
                .put( RequestBody.create(new byte[0], null))
                .build();

        requestSender.executeRequestWithValidation(request, "切换活跃模型失败");
    }

    @Override
    public void deleteWeight(String baseUrl, String filename) {
        String url = UrlUtils.buildUrl(baseUrl, "/weights/" + filename);
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        requestSender.executeRequestWithValidation(request, "删除物理模型失败");
    }

    // ================= 参数管理 =================

    @Override
    public void updateParamsBatch(String baseUrl, Map<String, Object> params) {
        String url = UrlUtils.buildUrl(baseUrl, "/params/");
        
        try {
            RequestBody body = RequestBody.create(jsonMapper.writeValueAsString(Map.of("params", params)), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            requestSender.executeRequestWithValidation(request, "批量更新模型参数失败");
        } catch (Exception e) {
            log.error("JSON serialization failed", e);
            throw new BizException("参数序列化失败");
        }
    }

    @Override
    public void updateParamSingle(String baseUrl, String key, Object value) {
        String url = UrlUtils.buildUrl(baseUrl, "/params/" + key);
        try {
            RequestBody body = RequestBody.create(jsonMapper.writeValueAsString(Map.of("value", value)), MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();

            requestSender.executeRequestWithValidation(request, "更新单个参数失败");
        } catch (Exception e) {
            log.error("JSON serialization failed", e);
            throw new BizException("参数序列化失败");
        }
    }

    @Override
    public void deleteParam(String baseUrl, String key) {
        String url = UrlUtils.buildUrl(baseUrl, "/params/" + key);
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        requestSender.executeRequestWithValidation(request, "删除模型参数失败");
    }


}