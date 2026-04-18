package com.tf.backend.core.application.domain.yolo.impl;

import com.tf.backend.core.application.strategy.InferenceResultParserStrategy;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.common.enumeration.TaskStatus;
import com.tf.backend.core.common.enumeration.TaskType;
import com.tf.backend.core.common.exception.BizException;
import com.tf.backend.core.common.util.IdUtils;
import com.tf.backend.core.common.util.UrlUtils;
import com.tf.backend.core.application.domain.yolo.YoloInferenceService;
import com.tf.backend.core.application.infrastructure.repo.YoloDetectionRecordService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.model.entity.YoloDetectionRecordEntity;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YoloInferenceServiceImpl implements YoloInferenceService {

    private final YoloNodeService yoloNodeService;

    private final YoloDetectionRecordService yoloDetectionRecordService;

    private final OkHttpClient inferenceOkHttpClient;

    private final JsonMapper jsonMapper;

    // 策略注册表
    private final Map<TaskType, InferenceResultParserStrategy> parserStrategyMap;


    // 构造器注入
    public YoloInferenceServiceImpl(
            YoloNodeService yoloNodeService,
            YoloDetectionRecordService yoloDetectionRecordService,
            @Qualifier("inferenceOkHttpClient") OkHttpClient inferenceOkHttpClient,
            JsonMapper jsonMapper,
            List<InferenceResultParserStrategy> strategies
    ) {
        this.yoloNodeService = yoloNodeService;
        this.yoloDetectionRecordService = yoloDetectionRecordService;
        this.inferenceOkHttpClient = inferenceOkHttpClient;
        this.jsonMapper = jsonMapper;
        // 将 List 转换为以 TaskType 为 Key 的 Map，方便后续 $O(1)$ 查找
        this.parserStrategyMap = strategies.stream()
                .collect(Collectors.toMap(InferenceResultParserStrategy::getSupportedType, Function.identity()));
    }

    @Override
    public YoloDetectionRecordEntity predictImage(Long nodeId, MultipartFile file) {
        return executeInference(nodeId, file, TaskType.IMAGE, "/image/predict");
    }

    @Override
    public YoloDetectionRecordEntity predictVideo(Long nodeId, MultipartFile file) {
        return executeInference(nodeId, file, TaskType.VIDEO, "/video/predict");
    }

    private YoloDetectionRecordEntity executeInference(Long nodeId, MultipartFile file, TaskType taskType, String endpoint) {
        String baseUrl = yoloNodeService.getBaseUrlById(nodeId);
        String code = IdUtils.uuid();

        YoloDetectionRecordEntity record = YoloDetectionRecordEntity.builder()
                .nodeId(nodeId)
                .code(code)
                .taskType(taskType) // 假设 TaskType 枚举有 getValue() 返回 Integer
            .taskStatus(TaskStatus.PENDING)
                .originalFilename(file.getOriginalFilename())
                .status(Status.DISABLED)
                .detectCount(0)
                .build();

        yoloDetectionRecordService.save(record);

        // 进入执行阶段后更新为处理中，便于前端中间态感知。
        record.setTaskStatus(TaskStatus.RUNNING);
        yoloDetectionRecordService.updateById(record);

        long startTime = System.currentTimeMillis();
        try {
            RequestBody fileBody = RequestBody.create(
                    file.getBytes(),
                    MediaType.parse(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
            );

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(UrlUtils.buildUrl(baseUrl, endpoint))
                    .post(requestBody)
                    .build();

            try (Response response = inferenceOkHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BizException("物理机处理失败, HTTP 状态码: " + response.code());
                }

                JsonNode rootNode = jsonMapper.readTree(response.body().string());

                if (rootNode.has("code") && rootNode.get("code").asInt() != 200) {
                    String errorMsg = rootNode.has("msg") ? rootNode.get("msg").asString() : "未知错误";
                    throw new BizException("算法节点抛出业务异常: " + errorMsg);
                }

                JsonNode dataNode = rootNode.get("data");

                if (dataNode != null) {
                    // 调用策略模式进行解析
                    InferenceResultParserStrategy strategy = parserStrategyMap.get(taskType);

                    if (strategy != null) {
                        strategy.parseAndFill(record, dataNode);
                    } else {
                        log.warn("No matching parser strategy found for TaskType: {}", taskType);
                    }
                }

                record.setTaskStatus(TaskStatus.SUCCESS);
                record.setStatus(Status.ENABLED);
            }
        } catch (Exception e) {
            log.error("Inference task [{}] failed. Node: {}", code, baseUrl, e);

            record.setTaskStatus(TaskStatus.FAILED);
            record.setStatus(Status.DISABLED);
            record.setErrorMessage(e.getMessage());
        } finally {
            record.setDurationMs(System.currentTimeMillis() - startTime);
            yoloDetectionRecordService.updateById(record);
        }

        return record;
    }
}
