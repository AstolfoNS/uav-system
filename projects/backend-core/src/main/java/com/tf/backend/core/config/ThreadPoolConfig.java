package com.tf.backend.core.config;

import com.tf.backend.core.config.property.MinioProperties;
import com.tf.backend.core.config.property.ThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ThreadPoolConfig {

    private final ThreadPoolProperties props;

    private final MinioProperties minioProps;


    @Bean
    public Executor minioFileExecutor() {
        log.info("初始化专属 minio 文件处理异步线程池: minioFileExecutor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(minioProps.getFileExecutor().getCorePoolSize());
        // 最大线程数 (动态计算)
        executor.setMaxPoolSize(Math.max(minioProps.getFileExecutor().getMaxPoolSize(), Runtime.getRuntime().availableProcessors()));
        // 队列容量
        executor.setQueueCapacity(minioProps.getFileExecutor().getQueueCapacity());
        // 线程空闲后的存活时间 (默认就是 60 秒)
        executor.setKeepAliveSeconds(minioProps.getFileExecutor().getKeepAliveSeconds());
        // 线程名称前缀，方便排查日志
        executor.setThreadNamePrefix(minioProps.getFileExecutor().getThreadNamePrefix());
        // 当队列满且线程数达到最大时，由提交任务的线程（比如 Tomcat 的 HTTP 线程）自己去执行该任务。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机配置
        // 等待所有仍在队列中和正在执行的任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最长等待时间，超过这个时间如果还没执行完则强制关闭
        executor.setAwaitTerminationSeconds(minioProps.getFileExecutor().getAwaitTerminationSeconds());
        // 初始化
        executor.initialize();

        return executor;
    }

    /**
     * 专属数据库/常规轻量级异步任务线程池
     */
    @Bean
    public Executor dbExecutor() {
        log.info("初始化专属数据库异步线程池: dbExecutor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：即使空闲也会保留的线程数（由于更新DB速度快，不用设太大，按服务器 CPU 核数来即可，比如 4 或 8）
        executor.setCorePoolSize(props.getDbExecutor().getCorePoolSize());
        // 最大线程数：当核心线程都在忙，且队列满了之后，允许扩展到的最大线程数
        executor.setMaxPoolSize(Math.max(props.getDbExecutor().getMaxPoolSize(), Runtime.getRuntime().availableProcessors()));
        // 队列容量：用来缓冲执行任务的队列。比如并发大时，先把任务放队列里排队
        executor.setQueueCapacity(props.getDbExecutor().getQueueCapacity());
        // 空闲线程存活时间（秒）：超出核心线程数的那部分线程，如果空闲这么久就会被销毁
        executor.setKeepAliveSeconds(props.getDbExecutor().getKeepAliveSeconds());
        // 线程名称前缀：极其重要！出了 Bug 看日志时，一眼就能认出是这个线程池干的
        executor.setThreadNamePrefix("db-async-");
        // 拒绝策略
        // CallerRunsPolicy：当线程池和队列都满了，不再接收新任务时，交由“调用者所在的线程（即主线程）”来直接执行该任务。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化
        executor.initialize();

        return executor;
    }
}