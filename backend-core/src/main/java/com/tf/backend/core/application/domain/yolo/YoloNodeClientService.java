package com.tf.backend.core.application.domain.yolo;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * 与底层 YOLO 推理节点 (FastAPI) 交互的客户端服务
 */
public interface YoloNodeClientService {

    // ================= 权重管理 (Weights) =================

    /**
     * 上传新模型权重文件到节点
     */
    void uploadWeight(String baseUrl, MultipartFile file);

    /**
     * 切换节点当前使用的活跃模型
     */
    void changeActiveWeight(String baseUrl, String filename);

    /**
     * 删除节点上的物理模型文件
     */
    void deleteWeight(String baseUrl, String filename);

    // ================= 参数管理 (Params) =================

    /**
     * 批量合并/更新节点的推理参数
     */
    void updateParamsBatch(String baseUrl, Map<String, Object> params);

    /**
     * 更新节点的单个推理参数
     */
    void updateParamSingle(String baseUrl, String key, Object value);

    /**
     * 删除/归零节点的指定推理参数
     */
    void deleteParam(String baseUrl, String key);
}