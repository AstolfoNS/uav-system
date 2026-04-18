package com.tf.backend.core.application.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tf.backend.core.common.enumeration.Status;
import com.tf.backend.core.application.domain.yolo.YoloNodeSyncService;
import com.tf.backend.core.application.infrastructure.repo.YoloNodeService;
import com.tf.backend.core.model.entity.YoloNodeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * YOLO 节点启动初始化执行器
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class YoloNodeStartupRunner {

    private final YoloNodeSyncService yoloNodeSyncService;

    private final YoloNodeService yoloNodeService;

    /**
     * 监听 Spring Boot 启动完成事件
     * 此时所有的 Bean 已经装载完毕，Web 服务器也已启动就绪
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Spring Boot 启动就绪，开始后台执行 YOLO 节点状态拉齐任务...");

        // 使用异步执行，坚决不阻塞主线程的启动进度
        CompletableFuture.runAsync(() -> {
            try {
                // 先执行一次全量健康检查，校准当前所有节点的真实在线状态
                log.info("Startup Step 1: Checking health for all configured nodes...");
                yoloNodeSyncService.checkAllNodesHealth();

                // 获取当前被判定为在线 (ENABLED) 的所有节点
                List<YoloNodeEntity> onlineNodes = yoloNodeService.list(
                        new LambdaQueryWrapper<YoloNodeEntity>()
                                .eq(YoloNodeEntity::getStatus, Status.ENABLED)
                );

                //对于那些一直保持在线的节点，强制进行一次全量物理数据同步
                // 为了防止服务离线期间物理机参数被偷偷修改，这里强制全部覆写对齐一次。
                if (!onlineNodes.isEmpty()) {
                    log.info("Startup Step 2: Force syncing physical state for {} online nodes...", onlineNodes.size());
                    
                    onlineNodes.parallelStream().forEach(node -> {
                        try {
                            yoloNodeSyncService.syncNodeState(node.getId());
                            log.info("[SUCCESS] Node [{}] startup sync completed.", node.getNodeName());
                        } catch (Exception e) {
                            log.error("[FAILED] Node [{}] startup sync failed: {}", node.getNodeName(), e.getMessage());
                        }
                    });
                } else {
                    log.warn("Startup Step 2: No online YOLO nodes found to sync.");
                }
                log.info("YOLO 节点状态拉齐任务结束");
            } catch (Exception e) {
                log.error("YOLO 节点启动初始化任务发生未捕获异常", e);
            }
        });
    }
}