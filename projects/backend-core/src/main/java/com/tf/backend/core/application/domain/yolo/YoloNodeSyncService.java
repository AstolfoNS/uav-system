package com.tf.backend.core.application.domain.yolo;

public interface YoloNodeSyncService {

    /**
     * 对所有注册节点进行健康探活（HTTP心跳检测）
     */
    void checkAllNodesHealth();

    /**
     * 以 Python 端 /weights 和 /params 接口的数据为 Truth Source 同步状态
     * @param nodeId 节点ID
     */
    void syncNodeState(Long nodeId);

}
