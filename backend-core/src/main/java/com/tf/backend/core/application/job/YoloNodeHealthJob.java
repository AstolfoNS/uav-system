package com.tf.backend.core.application.job;

import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoloNodeHealthJob {

    private final YoloNodeSyncService yoloNodeSyncService;

    /**
     * 每30秒执行一次Yolo推理节点健康探活
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void executeHealthCheck() {
        log.debug("Starting scheduled Yolo node health check...");
        yoloNodeSyncService.checkAllNodesHealth();
        log.debug("Finished Yolo node health check.");
    }
}
