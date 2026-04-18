package com.tf.backend.core.network;

import com.tf.backend.core.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class RequestSender {

    private final OkHttpClient okHttpClient;

    private final JsonMapper jsonMapper;

    /**
     * { "code": 200, "message": "..." }
     */
    public void executeRequestWithValidation(Request request, String errorPrefix) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 处理 4xx, 5xx HTTP 状态码
                String errorMsg = errorPrefix + " (HTTP Status: " + response.code() + ")";
                log.error("Node API Error Response: {}", response.body().string());
                throw new BizException(errorMsg);
            }

            // 解析 JSON 响应体
            JsonNode rootNode = jsonMapper.readTree(response.body().string());

            // 校验 FastAPI 约定的业务状态码
            if (rootNode.has("code") && rootNode.get("code").asInt() != 200) {
                String apiMessage = rootNode.has("message") ? rootNode.get("message").asString() : "未知节点错误";
                if (rootNode.has("msg")) {
                    apiMessage = rootNode.get("msg").asString();
                }
                throw new BizException(errorPrefix + ": " + apiMessage);
            }
        } catch (BizException e) {
            // BizException 直接抛出，不进行包装
            throw e;
        } catch (Exception e) {
            log.error("Network or parsing error while calling Node API", e);
            throw new BizException(errorPrefix + ": 网络通信或解析错误");
        }
    }
}
