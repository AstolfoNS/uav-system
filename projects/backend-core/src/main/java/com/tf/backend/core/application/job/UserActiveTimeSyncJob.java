package com.tf.backend.core.application.job;

import com.tf.backend.core.application.infrastructure.cache.RedisManager;
import com.tf.backend.core.application.infrastructure.repo.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserActiveTimeSyncJob {

    private final RedisManager redisManager;

    private final UserService userService;

    private static final String ACTIVE_TIME_KEY = "sys:user_active_time";

    /**
     * 每 10 分钟执行一次 (cron = "0 0/10 * * * ?")
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void syncActiveTime() {
        // 一次性获取所有活跃记录
        Map<Object, Object> entries = redisManager.hGetAll(ACTIVE_TIME_KEY);
        if (entries.isEmpty()) {
            return;
        }
        // 拿到数据后，立刻删除 Redis 里的 Key，防止下一秒的活跃数据丢失（重新生成一个空的 Hash）
        redisManager.delete(ACTIVE_TIME_KEY);
        // 数据转换
        Map<Long, LocalDateTime> updateMap = new HashMap<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                updateMap.put(
                        Long.parseLong(entry.getKey().toString()),
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(entry.getValue().toString())), ZoneId.systemDefault())
                );
            } catch (Exception e) {
                log.error("解析用户活跃时间异常 key: {}, value: {}", entry.getKey(), entry.getValue(), e);
            }
        }
        // 批量落库
        if (!updateMap.isEmpty()) {
            long startTime = System.currentTimeMillis();
            userService.batchUpdateLastActiveTime(updateMap);
            log.info("同步用户最后在线时间成功, 涉及用户数: {}, 耗时: {}ms", updateMap.size(), System.currentTimeMillis() - startTime);
        }
    }
}