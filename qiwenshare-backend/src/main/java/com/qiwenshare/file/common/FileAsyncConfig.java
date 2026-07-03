package com.qiwenshare.file.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文件模块异步线程池配置。
 *
 * <p>用于永久删除、审计日志写入等异步操作。
 * IO 密集型任务，线程数适当设大。</p>
 */
@Configuration
@EnableAsync
public class FileAsyncConfig {

    /**
     * 文件任务异步执行器。
     *
     * @param core  核心线程数
     * @param max   最大线程数
     * @param queue 队列容量
     * @return TaskExecutor
     */
    @Bean("fileTaskExecutor")
    public Executor fileTaskExecutor(
            @Value("${async.file.core-pool-size:4}") int core,
            @Value("${async.file.max-pool-size:8}") int max,
            @Value("${async.file.queue-capacity:100}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("file-task-");
        executor.initialize();
        return executor;
    }
}
